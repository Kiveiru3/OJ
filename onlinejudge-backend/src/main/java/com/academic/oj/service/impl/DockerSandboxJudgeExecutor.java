package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.config.JudgeProperties;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.TestCase;
import com.academic.oj.util.OutputComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
@Component
@RequiredArgsConstructor
public class DockerSandboxJudgeExecutor {

    private static final String WORK_DIR = "/workspace";

    private final JudgeProperties judgeProperties;
    private volatile Boolean dockerAvailable;

    public boolean isEnabled() {
        JudgeProperties.Sandbox sandbox = judgeProperties.getSandbox();
        return sandbox != null && sandbox.isEnabled();
    }

    public boolean isStrict() {
        JudgeProperties.Sandbox sandbox = judgeProperties.getSandbox();
        return sandbox != null && sandbox.isStrict();
    }

    public Submission judge(Submission submission, Problem problem, List<TestCase> testCases) throws Exception {
        if (!isDockerAvailable()) {
            throw new IllegalStateException("Docker CLI unavailable");
        }
        return switch (submission.getLanguage()) {
            case Constants.LANGUAGE_JAVA -> judgeJava(submission, problem, testCases);
            case Constants.LANGUAGE_CPP -> judgeCpp(submission, problem, testCases);
            case Constants.LANGUAGE_PYTHON -> judgePython(submission, problem, testCases);
            default -> {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage("Unsupported language: " + submission.getLanguage());
                yield submission;
            }
        };
    }

    private Submission judgeJava(Submission submission, Problem problem, List<TestCase> testCases) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_sandbox_");
            Files.writeString(tempDir.resolve("Main.java"), submission.getCode(), StandardCharsets.UTF_8);

            ProcessResult compile = runDockerCommand(
                    tempDir,
                    resolveJavaImage(),
                    List.of("sh", "-lc", "javac Main.java"),
                    null,
                    resolveCompileTimeout(),
                    resolveMemoryLimit(problem)
            );
            if (compile.timedOut()) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage("Compile timeout");
                return submission;
            }
            if (compile.exitCode() != 0) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage(trimOutput(compile.output()));
                return submission;
            }

            long timeoutMs = resolveTimeLimit(problem);
            int memoryLimitMb = resolveMemoryLimit(problem);
            for (TestCase testCase : testCases) {
                ProcessResult run = runDockerCommand(
                        tempDir,
                        resolveJavaImage(),
                        List.of("sh", "-lc", "java -Xms16m -Xmx" + memoryLimitMb + "m Main"),
                        safeInput(testCase.getInput()),
                        timeoutMs,
                        memoryLimitMb
                );
                if (run.timedOut()) {
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    return submission;
                }
                if (run.exitCode() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage(trimOutput(run.output()));
                    return submission;
                }
                if (!isAnswerAccepted(testCase.getOutput(), run.output())) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    submission.setErrorMessage("Expected: " + OutputComparator.preview(testCase.getOutput(), 300) + ", Got: " + OutputComparator.preview(run.output(), 300));
                    return submission;
                }
            }

            markAccepted(submission, problem);
            return submission;
        } catch (Exception e) {
            log.error("Java sandbox judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage("Sandbox error: " + safeTrim(e.getMessage()));
            return submission;
        } finally {
            cleanup(tempDir);
        }
    }

    private Submission judgeCpp(Submission submission, Problem problem, List<TestCase> testCases) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_sandbox_");
            Files.writeString(tempDir.resolve("main.cpp"), submission.getCode(), StandardCharsets.UTF_8);

            ProcessResult compile = runDockerCommand(
                    tempDir,
                    resolveCppImage(),
                    List.of("sh", "-lc", "g++ main.cpp -O2 -std=c++17 -o main"),
                    null,
                    resolveCompileTimeout(),
                    resolveMemoryLimit(problem)
            );
            if (compile.timedOut()) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage("Compile timeout");
                return submission;
            }
            if (compile.exitCode() != 0) {
                submission.setStatus(Constants.STATUS_COMPILE_ERROR);
                submission.setErrorMessage(trimOutput(compile.output()));
                return submission;
            }

            long timeoutMs = resolveTimeLimit(problem);
            int memoryLimitMb = resolveMemoryLimit(problem);
            for (TestCase testCase : testCases) {
                ProcessResult run = runDockerCommand(
                        tempDir,
                        resolveCppImage(),
                        List.of("sh", "-lc", "./main"),
                        safeInput(testCase.getInput()),
                        timeoutMs,
                        memoryLimitMb
                );
                if (run.timedOut()) {
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    return submission;
                }
                if (run.exitCode() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage(trimOutput(run.output()));
                    return submission;
                }
                if (!isAnswerAccepted(testCase.getOutput(), run.output())) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    submission.setErrorMessage("Expected: " + OutputComparator.preview(testCase.getOutput(), 300) + ", Got: " + OutputComparator.preview(run.output(), 300));
                    return submission;
                }
            }

            markAccepted(submission, problem);
            return submission;
        } catch (Exception e) {
            log.error("Cpp sandbox judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage("Sandbox error: " + safeTrim(e.getMessage()));
            return submission;
        } finally {
            cleanup(tempDir);
        }
    }

    private Submission judgePython(Submission submission, Problem problem, List<TestCase> testCases) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("judge_sandbox_");
            Files.writeString(tempDir.resolve("solution.py"), submission.getCode(), StandardCharsets.UTF_8);

            long timeoutMs = resolveTimeLimit(problem);
            int memoryLimitMb = resolveMemoryLimit(problem);
            for (TestCase testCase : testCases) {
                ProcessResult run = runDockerCommand(
                        tempDir,
                        resolvePythonImage(),
                        List.of("sh", "-lc", "python solution.py"),
                        safeInput(testCase.getInput()),
                        timeoutMs,
                        memoryLimitMb
                );
                if (run.timedOut()) {
                    submission.setStatus(Constants.STATUS_TIME_LIMIT_EXCEEDED);
                    return submission;
                }
                if (run.exitCode() != 0) {
                    submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    submission.setErrorMessage(trimOutput(run.output()));
                    return submission;
                }
                if (!isAnswerAccepted(testCase.getOutput(), run.output())) {
                    submission.setStatus(Constants.STATUS_WRONG_ANSWER);
                    submission.setErrorMessage("Expected: " + OutputComparator.preview(testCase.getOutput(), 300) + ", Got: " + OutputComparator.preview(run.output(), 300));
                    return submission;
                }
            }

            markAccepted(submission, problem);
            return submission;
        } catch (Exception e) {
            log.error("Python sandbox judge error", e);
            submission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            submission.setErrorMessage("Sandbox error: " + safeTrim(e.getMessage()));
            return submission;
        } finally {
            cleanup(tempDir);
        }
    }

    private ProcessResult runDockerCommand(Path workDir, String image, List<String> shellCommand,
                                           String stdin, long timeoutMs, int memoryLimitMb) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(resolveDockerBinary());
        command.add("run");
        command.add("--rm");
        // Keep STDIN open for run stage; otherwise containerized programs read EOF immediately.
        if (stdin != null) {
            command.add("-i");
        }
        command.add("--network");
        command.add("none");
        command.add("--read-only");
        command.add("--cap-drop");
        command.add("ALL");
        command.add("--security-opt");
        command.add("no-new-privileges");
        command.add("--memory");
        command.add(memoryLimitMb + "m");
        command.add("--cpus");
        command.add(resolveCpus());
        command.add("--pids-limit");
        command.add(String.valueOf(resolvePidsLimit()));
        command.add("--tmpfs");
        command.add("/tmp:rw,size=" + resolveTmpfsSize());
        command.add("-v");
        command.add(workDir.toAbsolutePath() + ":" + WORK_DIR);
        command.add("-w");
        command.add(WORK_DIR);
        command.add(image);
        command.addAll(shellCommand);
        return executeProcess(command, null, stdin, timeoutMs);
    }

    private ProcessResult executeProcess(List<String> command, File workDir, String stdin, long timeoutMs)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null) {
            pb.directory(workDir);
        }
        pb.redirectErrorStream(true);

        Process process = pb.start();
        if (stdin != null) {
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } else {
            process.getOutputStream().close();
        }

        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = readerExecutor.submit(() -> readProcessOutput(process));

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        String output = "";
        try {
            output = outputFuture.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            log.warn("Failed to capture process output, command={}", command, e);
        } finally {
            readerExecutor.shutdownNow();
        }

        int exitCode = finished ? process.exitValue() : -1;
        return new ProcessResult(exitCode, output, !finished);
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
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

    private long resolveCompileTimeout() {
        JudgeProperties.Sandbox sandbox = judgeProperties.getSandbox();
        if (sandbox != null && sandbox.getCompileTimeout() != null && sandbox.getCompileTimeout() > 0) {
            return sandbox.getCompileTimeout();
        }
        return 15000L;
    }

    private int resolveMemoryLimit(Problem problem) {
        Integer rawLimit = problem.getMemoryLimit();
        if (rawLimit != null && rawLimit > 0) {
            int normalizedMb = rawLimit > 8192 ? Math.max(1, (rawLimit + 1023) / 1024) : rawLimit;
            return Math.max(16, Math.min(normalizedMb, 2048));
        }
        int fallback = judgeProperties.getMaxMemory() != null && judgeProperties.getMaxMemory() > 0
                ? judgeProperties.getMaxMemory()
                : 256;
        return Math.max(16, Math.min(fallback, 2048));
    }

    private void markAccepted(Submission submission, Problem problem) {
        submission.setStatus(Constants.STATUS_ACCEPTED);
        submission.setTimeUsed((int) Math.max(1, resolveTimeLimit(problem) / 2));
        submission.setMemoryUsed(Math.max(1, resolveMemoryLimit(problem) / 2));
    }

    private boolean isAnswerAccepted(String expected, String actual) {
        return OutputComparator.equalsIgnorePresentation(expected, actual);
    }

    private String safeInput(String input) {
        return input == null ? "" : input;
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private String trimOutput(String text) {
        String value = safeTrim(text);
        if (value.length() <= 3000) {
            return value;
        }
        return value.substring(0, 3000) + "...(truncated)";
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

    private boolean isDockerAvailable() {
        if (dockerAvailable != null) {
            return dockerAvailable;
        }
        synchronized (this) {
            if (dockerAvailable != null) {
                return dockerAvailable;
            }
            try {
                ProcessResult result = executeProcess(List.of(resolveDockerBinary(), "--version"), null, null, 3000);
                dockerAvailable = result.exitCode() == 0;
            } catch (Exception e) {
                dockerAvailable = false;
            }
            return dockerAvailable;
        }
    }

    private String resolveDockerBinary() {
        String value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getDockerBinary();
        return value == null || value.isBlank() ? "docker" : value;
    }

    private String resolveJavaImage() {
        String value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getJavaImage();
        return value == null || value.isBlank() ? "eclipse-temurin:17-jdk" : value;
    }

    private String resolveCppImage() {
        String value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getCppImage();
        return value == null || value.isBlank() ? "gcc:13" : value;
    }

    private String resolvePythonImage() {
        String value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getPythonImage();
        return value == null || value.isBlank() ? "python:3.11-alpine" : value;
    }

    private String resolveCpus() {
        String value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getCpus();
        return value == null || value.isBlank() ? "1.0" : value;
    }

    private int resolvePidsLimit() {
        Integer value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getPidsLimit();
        return value == null ? 128 : Math.max(value, 16);
    }

    private String resolveTmpfsSize() {
        String value = judgeProperties.getSandbox() == null ? null : judgeProperties.getSandbox().getTmpfsSize();
        return value == null || value.isBlank() ? "64m" : value;
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

    private record ProcessResult(int exitCode, String output, boolean timedOut) {
    }
}
