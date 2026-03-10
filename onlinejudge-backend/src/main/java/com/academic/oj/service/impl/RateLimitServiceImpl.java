package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.config.RateLimitProperties;
import com.academic.oj.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public void checkLoginLimit(String clientIp) {
        if (!rateLimitProperties.isEnabled()) {
            return;
        }
        Integer limit = rateLimitProperties.getLoginPerMinute();
        if (limit == null || limit <= 0) {
            return;
        }
        String safeIp = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        assertAllowed("rl:login:" + safeIp, limit, "登录过于频繁，请稍后再试");
    }

    @Override
    public void checkSubmitLimit(Long userId) {
        if (!rateLimitProperties.isEnabled()) {
            return;
        }
        Integer limit = rateLimitProperties.getSubmitPerMinute();
        if (limit == null || limit <= 0 || userId == null) {
            return;
        }
        assertAllowed("rl:submit:user:" + userId, limit, "提交过于频繁，请稍后再试");
    }

    private void assertAllowed(String key, int limit, String message) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            if (count != null && count > limit) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), message);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            // Fail-open: avoid blocking core flow when redis is temporary unavailable.
            log.warn("Rate limit unavailable for key {}: {}", key, ex.getMessage());
        }
    }
}
