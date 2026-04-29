package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.config.JudgeProperties;
import com.academic.oj.dto.SubmissionCaseResultDTO;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.TestCase;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.service.JudgeService;
import com.academic.oj.util.OutputComparator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final JudgeProperties judgeProperties;
    private final DockerSandboxJudgeExecutor dockerSandboxJudgeExecutor;

    @Override
    public Submission judge(Submission submission) {
        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem == null) {
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage("Problem not found");
            return submission;
        }

        List<TestCase> testCases = testCaseMapper.selectList(
                new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getProblemId, submission.getProblemId())
        );
        testCases = resolveJudgeTestCases(problem, testCases);
        if (testCases.isEmpty()) {
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage("No test cases found");
            return submission;
        }

        if (dockerSandboxJudgeExecutor.isEnabled()) {
            try {
                return dockerSandboxJudgeExecutor.judge(submission, problem, testCases);
            } catch (Exception e) {
                log.error("Sandbox judge error", e);
                if (dockerSandboxJudgeExecutor.isStrict()) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage("Sandbox judge failed: " + safeTrim(e.getMessage()));
                    return submission;
                }
                log.warn("Fallback to local judge. submissionId={}, language={}", submission.getId(), submission.getLanguage());
            }
        }

        try {
            switch (submission.getLanguage()) {
                case Constants.LANGUAGE_JAVA:
                    return judgeJava(submission, problem, testCases);
                case Constants.LANGUAGE_CPP:
                    return judgeCpp(submission, problem, testCases);
                case Constants.LANGUAGE_PYTHON:
                    return judgePython(submission, problem, testCases);
                default:
                    submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                    submission.setErrorMessage("Unsupported language: " + submission.getLanguage());
                    return submission;
            }
        } catch (Exception e) {
            log.error("Judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage("Judge error: " + e.getMessage());
            return submission;
        }
    }

    private Submission judgeJava(Submission submission, Problem problem, List<TestCase> testCases) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_");
            String className = "Main";
            File sourceFile = new File(tempDir.toFile(), className + ".java");
            Files.writeString(sourceFile.toPath(), submission.getCode(), StandardCharsets.UTF_8);

            ProcessBuilder compilePb = new ProcessBuilder(
                    judgeProperties.getJavaCompiler(),
                    sourceFile.getAbsolutePath()
            );
            compilePb.directory(tempDir.toFile());
            ProcessExecutionResult compileResult = executeProcess(compilePb, null, resolveCompileTimeoutMs());
            if (compileResult.timedOut()) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage("Compile timeout");
                return submission;
            }
            if (compileResult.exitCode() != 0) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage(compileResult.output());
                return submission;
            }

            long timeoutMs = resolveTimeLimit(problem);
            int memoryLimitMb = resolveMemoryLimit(problem);
            int maxTimeUsedMs = 1;
            int caseIndex = 0;
            List<SubmissionCaseResultDTO> caseResults = new ArrayList<>();
            submission.setCaseResults(caseResults);
            for (TestCase testCase : testCases) {
                caseIndex++;
                ProcessBuilder runPb = new ProcessBuilder(
                        judgeProperties.getJavaRuntime(),
                        "-Xms16m",
                        "-Xmx" + memoryLimitMb + "m",
                        className
                );
                runPb.directory(tempDir.toFile());
                ProcessExecutionResult runResult = executeProcess(runPb, safeInput(testCase.getInput()), timeoutMs);
                if (runResult.timedOut()) {
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_TIME_LIMIT_EXCEEDED,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), null,
                            "超过时间限制 " + timeoutMs + "ms"));
                    submission.setErrorMessage(buildTimeLimitMessage(caseIndex, testCase, timeoutMs));
                    return submission;
                }
                if (runResult.exitCode() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_RUNTIME_ERROR,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), runResult.output(),
                            preview(runResult.output(), 300)));
                    submission.setErrorMessage(buildRuntimeErrorMessage(caseIndex, testCase, runResult.output()));
                    return submission;
                }
                maxTimeUsedMs = Math.max(maxTimeUsedMs, (int) Math.max(1L, runResult.elapsedMs()));

                if (!isAnswerAccepted(testCase.getOutput(), runResult.output())) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_WRONG_ANSWER,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), runResult.output(), "输出不匹配"));
                    submission.setErrorMessage(buildWrongAnswerMessage(caseIndex, testCase, runResult.output()));
                    return submission;
                }
                caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_ACCEPTED,
                        (int) Math.max(1L, runResult.elapsedMs()), null,
                        testCase.getOutput(), runResult.output(), null));
            }

            markAccepted(submission, problem, maxTimeUsedMs);
            return submission;
        } catch (IOException e) {
            submission.setStatus(Constants.STATUS_COMPILE_ERROR);
            submission.setErrorMessage("Java toolchain unavailable: " + e.getMessage());
            return submission;
        } catch (Exception e) {
            log.error("Java judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage(e.getMessage());
            return submission;
        } finally {
            cleanup(tempDir);
        }
    }

    private Submission judgeCpp(Submission submission, Problem problem, List<TestCase> testCases) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_");
            File sourceFile = new File(tempDir.toFile(), "main.cpp");
            File execFile = new File(tempDir.toFile(), "main.exe");
            Files.writeString(sourceFile.toPath(), submission.getCode(), StandardCharsets.UTF_8);

            ProcessBuilder compilePb = new ProcessBuilder(
                    judgeProperties.getCppCompiler(),
                    sourceFile.getAbsolutePath(),
                    "-O2",
                    "-std=c++17",
                    "-o",
                    execFile.getAbsolutePath()
            );
            compilePb.directory(tempDir.toFile());
            ProcessExecutionResult compileResult = executeProcess(compilePb, null, resolveCompileTimeoutMs());
            if (compileResult.timedOut()) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage("Compile timeout");
                return submission;
            }
            if (compileResult.exitCode() != 0) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage(compileResult.output());
                return submission;
            }

            long timeoutMs = resolveTimeLimit(problem);
            int maxTimeUsedMs = 1;
            int caseIndex = 0;
            List<SubmissionCaseResultDTO> caseResults = new ArrayList<>();
            submission.setCaseResults(caseResults);
            for (TestCase testCase : testCases) {
                caseIndex++;
                ProcessBuilder runPb = new ProcessBuilder(execFile.getAbsolutePath());
                runPb.directory(tempDir.toFile());
                ProcessExecutionResult runResult = executeProcess(runPb, safeInput(testCase.getInput()), timeoutMs);
                if (runResult.timedOut()) {
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_TIME_LIMIT_EXCEEDED,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), null,
                            "超过时间限制 " + timeoutMs + "ms"));
                    submission.setErrorMessage(buildTimeLimitMessage(caseIndex, testCase, timeoutMs));
                    return submission;
                }
                if (runResult.exitCode() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_RUNTIME_ERROR,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), runResult.output(),
                            preview(runResult.output(), 300)));
                    submission.setErrorMessage(buildRuntimeErrorMessage(caseIndex, testCase, runResult.output()));
                    return submission;
                }
                maxTimeUsedMs = Math.max(maxTimeUsedMs, (int) Math.max(1L, runResult.elapsedMs()));

                if (!isAnswerAccepted(testCase.getOutput(), runResult.output())) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_WRONG_ANSWER,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), runResult.output(), "输出不匹配"));
                    submission.setErrorMessage(buildWrongAnswerMessage(caseIndex, testCase, runResult.output()));
                    return submission;
                }
                caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_ACCEPTED,
                        (int) Math.max(1L, runResult.elapsedMs()), null,
                        testCase.getOutput(), runResult.output(), null));
            }

            markAccepted(submission, problem, maxTimeUsedMs);
            return submission;
        } catch (IOException e) {
            submission.setStatus(Constants.STATUS_COMPILE_ERROR);
            submission.setErrorMessage("C++ toolchain unavailable: " + e.getMessage());
            return submission;
        } catch (Exception e) {
            log.error("C++ judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage(e.getMessage());
            return submission;
        } finally {
            cleanup(tempDir);
        }
    }

    private Submission judgePython(Submission submission, Problem problem, List<TestCase> testCases) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_");
            File sourceFile = new File(tempDir.toFile(), "solution.py");
            Files.writeString(sourceFile.toPath(), submission.getCode(), StandardCharsets.UTF_8);

            long timeoutMs = resolveTimeLimit(problem);
            int maxTimeUsedMs = 1;
            int caseIndex = 0;
            List<SubmissionCaseResultDTO> caseResults = new ArrayList<>();
            submission.setCaseResults(caseResults);
            for (TestCase testCase : testCases) {
                caseIndex++;
                ProcessBuilder runPb = new ProcessBuilder(
                        judgeProperties.getPythonRuntime(),
                        sourceFile.getAbsolutePath()
                );
                runPb.directory(tempDir.toFile());
                ProcessExecutionResult runResult = executeProcess(runPb, safeInput(testCase.getInput()), timeoutMs);
                if (runResult.timedOut()) {
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_TIME_LIMIT_EXCEEDED,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), null,
                            "超过时间限制 " + timeoutMs + "ms"));
                    submission.setErrorMessage(buildTimeLimitMessage(caseIndex, testCase, timeoutMs));
                    return submission;
                }
                if (runResult.exitCode() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_RUNTIME_ERROR,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), runResult.output(),
                            preview(runResult.output(), 300)));
                    submission.setErrorMessage(buildRuntimeErrorMessage(caseIndex, testCase, runResult.output()));
                    return submission;
                }
                maxTimeUsedMs = Math.max(maxTimeUsedMs, (int) Math.max(1L, runResult.elapsedMs()));

                if (!isAnswerAccepted(testCase.getOutput(), runResult.output())) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_WRONG_ANSWER,
                            (int) Math.max(1L, runResult.elapsedMs()), null,
                            testCase.getOutput(), runResult.output(), "输出不匹配"));
                    submission.setErrorMessage(buildWrongAnswerMessage(caseIndex, testCase, runResult.output()));
                    return submission;
                }
                caseResults.add(buildCaseResult(caseIndex, testCase, Constants.STATUS_ACCEPTED,
                        (int) Math.max(1L, runResult.elapsedMs()), null,
                        testCase.getOutput(), runResult.output(), null));
            }

            markAccepted(submission, problem, maxTimeUsedMs);
            return submission;
        } catch (IOException e) {
            submission.setStatus(Constants.STATUS_COMPILE_ERROR);
            submission.setErrorMessage("Python runtime unavailable: " + e.getMessage());
            return submission;
        } catch (Exception e) {
            log.error("Python judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage(e.getMessage());
            return submission;
        } finally {
            cleanup(tempDir);
        }
    }

    private long resolveTimeLimit(Problem problem) {
        Integer problemLimit = problem.getTimeLimit();
        long base;
        if (problemLimit != null && problemLimit > 0) {
            base = problemLimit;
        } else {
            base = judgeProperties.getTimeout() != null && judgeProperties.getTimeout() > 0
                    ? judgeProperties.getTimeout()
                    : 5000L;
        }
        return applyTimeoutPolicy(base);
    }

    private int resolveMemoryLimit(Problem problem) {
        // Compatibility:
        // - Some data sources store memoryLimit in KB (e.g. 262144)
        // - Some store it directly in MB (e.g. 256)
        Integer rawLimit = problem.getMemoryLimit();
        if (rawLimit != null && rawLimit > 0) {
            int normalizedMb = rawLimit > 8192
                    ? Math.max(1, (rawLimit + 1023) / 1024)
                    : rawLimit;
            return Math.max(16, Math.min(normalizedMb, 2048));
        }
        int fallback = judgeProperties.getMaxMemory() != null && judgeProperties.getMaxMemory() > 0
                ? judgeProperties.getMaxMemory()
                : 256;
        return Math.max(16, Math.min(fallback, 2048));
    }

    private void markAccepted(Submission submission, Problem problem) {
        markAccepted(submission, problem, (int) Math.max(1, resolveTimeLimit(problem) / 2));
    }

    private void markAccepted(Submission submission, Problem problem, int timeUsedMs) {
        submission.setStatus(Constants.STATUS_ACCEPTED);
        submission.setTimeUsed(Math.max(1, timeUsedMs));
        submission.setMemoryUsed(Math.max(1, resolveMemoryLimit(problem) / 2));
    }

    private boolean isAnswerAccepted(String expected, String actual) {
        return OutputComparator.equalsIgnorePresentation(expected, actual);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeInput(String input) {
        return input == null ? "" : input;
    }

    private String buildTimeLimitMessage(int caseIndex, TestCase testCase, long timeoutMs) {
        return "测试点 " + caseName(caseIndex, testCase) + " 超时（>" + timeoutMs + "ms）\n"
                + "输入预览: " + preview(testCase == null ? null : testCase.getInput(), 200);
    }

    private String buildRuntimeErrorMessage(int caseIndex, TestCase testCase, String runtimeOutput) {
        return "测试点 " + caseName(caseIndex, testCase) + " 运行错误\n"
                + "输入预览: " + preview(testCase == null ? null : testCase.getInput(), 200) + "\n"
                + "错误输出: " + preview(runtimeOutput, 300);
    }

    private String buildWrongAnswerMessage(int caseIndex, TestCase testCase, String actualOutput) {
        return "测试点 " + caseName(caseIndex, testCase) + " 答案错误\n"
                + "输入预览: " + preview(testCase == null ? null : testCase.getInput(), 200) + "\n"
                + "期望输出: " + preview(testCase == null ? null : testCase.getOutput(), 300) + "\n"
                + "你的输出: " + preview(actualOutput, 300);
    }

    private String caseName(int caseIndex, TestCase testCase) {
        String type = Integer.valueOf(1).equals(testCase == null ? null : testCase.getIsSample()) ? "样例" : "隐藏";
        return "#" + caseIndex + "（" + type + "）";
    }

    private String preview(String text, int maxLen) {
        String value = safeTrim(text);
        if (value.isEmpty()) {
            return "<空>";
        }
        return OutputComparator.preview(value, maxLen);
    }

    private SubmissionCaseResultDTO buildCaseResult(int caseIndex,
                                                    TestCase testCase,
                                                    String status,
                                                    Integer timeUsed,
                                                    Integer memoryUsed,
                                                    String expected,
                                                    String actual,
                                                    String errorMessage) {
        SubmissionCaseResultDTO dto = new SubmissionCaseResultDTO();
        dto.setCaseNo(caseIndex);
        dto.setIsSample(Integer.valueOf(1).equals(testCase == null ? null : testCase.getIsSample()) ? 1 : 0);
        dto.setStatus(status);
        dto.setTimeUsed(timeUsed);
        dto.setMemoryUsed(memoryUsed);
        dto.setInputPreview(preview(testCase == null ? null : testCase.getInput(), 200));
        dto.setExpectedPreview(preview(expected, 300));
        dto.setActualPreview(preview(actual, 300));
        dto.setErrorMessage(errorMessage);
        return dto;
    }

    private long resolveCompileTimeoutMs() {
        if (judgeProperties != null
                && judgeProperties.getSandbox() != null
                && judgeProperties.getSandbox().getCompileTimeout() != null
                && judgeProperties.getSandbox().getCompileTimeout() > 0) {
            return judgeProperties.getSandbox().getCompileTimeout();
        }
        return 15000L;
    }

    private ProcessExecutionResult executeProcess(ProcessBuilder processBuilder, String stdin, long timeoutMs)
            throws IOException, InterruptedException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        if (stdin != null) {
            try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                writer.print(stdin);
                writer.flush();
            }
        } else {
            process.getOutputStream().close();
        }

        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = readerExecutor.submit(() -> readProcessOutput(process));

        long start = System.nanoTime();
        boolean finished = process.waitFor(Math.max(1L, timeoutMs), TimeUnit.MILLISECONDS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (!finished) {
            process.destroyForcibly();
        }

        String output = "";
        try {
            output = outputFuture.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            log.warn("Failed to capture process output", e);
        } finally {
            readerExecutor.shutdownNow();
        }

        int exitCode = finished ? process.exitValue() : -1;
        return new ProcessExecutionResult(exitCode, output, !finished, Math.max(1L, elapsedMs));
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private void cleanup(Path tempDir) {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup temp dir: {}", tempDir, e);
        }
    }

    private long applyTimeoutPolicy(long baseTimeoutMs) {
        long base = Math.max(1L, baseTimeoutMs);
        int scalePercent = judgeProperties.getTimeLimitScalePercent() != null
                ? Math.max(100, judgeProperties.getTimeLimitScalePercent())
                : 100;
        long extraMs = judgeProperties.getTimeLimitExtraMs() != null
                ? Math.max(0L, judgeProperties.getTimeLimitExtraMs())
                : 0L;

        long scaled = safeMultiply(base, scalePercent) / 100;
        long plusExtra = safeAdd(base, extraMs);
        return Math.max(base, Math.max(scaled, plusExtra));
    }

    private long safeMultiply(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    private long safeAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private List<TestCase> resolveJudgeTestCases(Problem problem, List<TestCase> testCases) {
        if (testCases != null && !testCases.isEmpty()) {
            List<TestCase> nonSampleCases = testCases.stream()
                    .filter(tc -> !Integer.valueOf(1).equals(tc.getIsSample()))
                    .toList();
            if (!nonSampleCases.isEmpty()) {
                return nonSampleCases;
            }
            return testCases;
        }

        if (!safeTrim(problem.getSampleInput()).isEmpty() || !safeTrim(problem.getSampleOutput()).isEmpty()) {
            TestCase fallback = new TestCase();
            fallback.setProblemId(problem.getId());
            fallback.setInput(problem.getSampleInput());
            fallback.setOutput(problem.getSampleOutput());
            fallback.setIsSample(1);
            List<TestCase> fallbackList = new ArrayList<>();
            fallbackList.add(fallback);
            return fallbackList;
        }

        return List.of();
    }

    private record ProcessExecutionResult(int exitCode, String output, boolean timedOut, long elapsedMs) {
    }
}
