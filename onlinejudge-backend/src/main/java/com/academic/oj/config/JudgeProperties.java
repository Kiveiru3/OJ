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
}
