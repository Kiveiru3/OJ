package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.config.JudgeProperties;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.TestCase;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.service.JudgeService;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final JudgeProperties judgeProperties;

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
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            String compileOutput = readProcessOutput(compileProcess);
            if (compileProcess.waitFor() != 0) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage(compileOutput);
                return submission;
            }

            long timeoutMs = resolveTimeLimit(problem);
            for (TestCase testCase : testCases) {
                ProcessBuilder runPb = new ProcessBuilder(
                        judgeProperties.getJavaRuntime(),
                        className
                );
                runPb.directory(tempDir.toFile());
                runPb.redirectErrorStream(true);
                Process runProcess = runPb.start();

                try (PrintWriter writer = new PrintWriter(runProcess.getOutputStream())) {
                    writer.print(testCase.getInput());
                    writer.flush();
                }

                String output = readProcessOutput(runProcess);
                boolean finished = runProcess.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) {
                    runProcess.destroyForcibly();
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    return submission;
                }
                if (runProcess.exitValue() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage(output);
                    return submission;
                }

                if (!isAnswerAccepted(testCase.getOutput(), output)) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    submission.setErrorMessage("Expected: " + safeTrim(testCase.getOutput()) + ", Got: " + safeTrim(output));
                    return submission;
                }
            }

            markAccepted(submission, problem);
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
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            String compileOutput = readProcessOutput(compileProcess);
            if (compileProcess.waitFor() != 0) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage(compileOutput);
                return submission;
            }

            long timeoutMs = resolveTimeLimit(problem);
            for (TestCase testCase : testCases) {
                ProcessBuilder runPb = new ProcessBuilder(execFile.getAbsolutePath());
                runPb.directory(tempDir.toFile());
                runPb.redirectErrorStream(true);
                Process runProcess = runPb.start();

                try (PrintWriter writer = new PrintWriter(runProcess.getOutputStream())) {
                    writer.print(testCase.getInput());
                    writer.flush();
                }

                String output = readProcessOutput(runProcess);
                boolean finished = runProcess.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) {
                    runProcess.destroyForcibly();
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    return submission;
                }
                if (runProcess.exitValue() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage(output);
                    return submission;
                }

                if (!isAnswerAccepted(testCase.getOutput(), output)) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    submission.setErrorMessage("Expected: " + safeTrim(testCase.getOutput()) + ", Got: " + safeTrim(output));
                    return submission;
                }
            }

            markAccepted(submission, problem);
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
            for (TestCase testCase : testCases) {
                ProcessBuilder runPb = new ProcessBuilder(
                        judgeProperties.getPythonRuntime(),
                        sourceFile.getAbsolutePath()
                );
                runPb.directory(tempDir.toFile());
                runPb.redirectErrorStream(true);
                Process runProcess = runPb.start();

                try (PrintWriter writer = new PrintWriter(runProcess.getOutputStream())) {
                    writer.print(testCase.getInput());
                    writer.flush();
                }

                String output = readProcessOutput(runProcess);
                boolean finished = runProcess.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) {
                    runProcess.destroyForcibly();
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    return submission;
                }
                if (runProcess.exitValue() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage(output);
                    return submission;
                }

                if (!isAnswerAccepted(testCase.getOutput(), output)) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    submission.setErrorMessage("Expected: " + safeTrim(testCase.getOutput()) + ", Got: " + safeTrim(output));
                    return submission;
                }
            }

            markAccepted(submission, problem);
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
        if (problemLimit != null && problemLimit > 0) {
            return problemLimit;
        }
        return judgeProperties.getTimeout() != null && judgeProperties.getTimeout() > 0
                ? judgeProperties.getTimeout()
                : 5000L;
    }

    private int resolveMemoryLimit(Problem problem) {
        Integer problemLimit = problem.getMemoryLimit();
        if (problemLimit != null && problemLimit > 0) {
            return problemLimit;
        }
        return judgeProperties.getMaxMemory() != null && judgeProperties.getMaxMemory() > 0
                ? judgeProperties.getMaxMemory()
                : 256;
    }

    private void markAccepted(Submission submission, Problem problem) {
        submission.setStatus(Constants.STATUS_ACCEPTED);
        submission.setTimeUsed((int) Math.max(1, resolveTimeLimit(problem) / 2));
        submission.setMemoryUsed(Math.max(1, resolveMemoryLimit(problem) / 2));
    }

    private boolean isAnswerAccepted(String expected, String actual) {
        return safeTrim(expected).equals(safeTrim(actual));
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
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
}
