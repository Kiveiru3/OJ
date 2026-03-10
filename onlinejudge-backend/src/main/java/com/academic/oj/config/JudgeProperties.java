package com.academic.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "judge")
public class JudgeProperties {
    /**
     * Fallback timeout (ms) when problem time limit is missing.
     */
    private Long timeout = 5000L;

    /**
     * Fallback memory limit (MB) when problem memory limit is missing.
     */
    private Integer maxMemory = 256;

    /**
     * Time limit multiplier in percent (e.g. 150 means 1.5x).
     */
    private Integer timeLimitScalePercent = 150;

    /**
     * Extra timeout in milliseconds to absorb sandbox/process overhead.
     */
    private Long timeLimitExtraMs = 800L;

    /**
     * Max number of concurrent judge workers.
     */
    private Integer maxConcurrentRuns = 4;

    /**
     * Java compiler executable.
     */
    private String javaCompiler = "javac";

    /**
     * Java runtime executable.
     */
    private String javaRuntime = "java";

    /**
     * C++ compiler executable.
     */
    private String cppCompiler = "g++";

    /**
     * Python runtime executable.
     */
    private String pythonRuntime = "python";

    /**
     * Sandbox configuration.
     */
    private Sandbox sandbox = new Sandbox();

    /**
     * Retry policy for transient judge failures.
     */
    private Retry retry = new Retry();

    @Data
    public static class Sandbox {
        /**
         * Enable docker sandbox for judge.
         */
        private boolean enabled = false;

        /**
         * If true, sandbox failure will not fallback to local judge.
         */
        private boolean strict = false;

        /**
         * Docker CLI binary name.
         */
        private String dockerBinary = "docker";

        /**
         * Docker image for Java judging.
         */
        private String javaImage = "eclipse-temurin:17-jdk";

        /**
         * Docker image for C++ judging.
         */
        private String cppImage = "gcc:13";

        /**
         * Docker image for Python judging.
         */
        private String pythonImage = "python:3.11-alpine";

        /**
         * vCPU quota passed to --cpus.
         */
        private String cpus = "1.0";

        /**
         * Max process count in container.
         */
        private Integer pidsLimit = 128;

        /**
         * Tmpfs size used by /tmp inside container.
         */
        private String tmpfsSize = "64m";

        /**
         * Compile timeout in milliseconds.
         */
        private Long compileTimeout = 15000L;
    }

    @Data
    public static class Retry {
        /**
         * Max attempts for one submission (>=1).
         */
        private Integer maxAttempts = 1;

        /**
         * Delay between retries in milliseconds.
         */
        private Long backoffMs = 0L;
    }
}
