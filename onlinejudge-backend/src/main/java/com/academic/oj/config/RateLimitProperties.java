package com.academic.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    /**
     * Global switch. Set false to disable all API rate limits.
     */
    private boolean enabled = true;

    /**
     * Max login attempts per minute for each client IP.
     */
    private Integer loginPerMinute = 30;

    /**
     * Max submissions per minute for each user.
     */
    private Integer submitPerMinute = 20;
}

