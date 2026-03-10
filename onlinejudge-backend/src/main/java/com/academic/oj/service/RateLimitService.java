package com.academic.oj.service;

public interface RateLimitService {
    void checkLoginLimit(String clientIp);

    void checkSubmitLimit(Long userId);
}

