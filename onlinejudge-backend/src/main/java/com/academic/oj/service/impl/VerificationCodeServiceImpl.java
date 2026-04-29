package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.VerificationCodeDTO;
import com.academic.oj.service.SmsSender;
import com.academic.oj.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH_BOUND = 1_000_000;
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final boolean exposeCodeInResponse;
    private final SmsSender smsSender;

    public VerificationCodeServiceImpl(
            @Value("${auth.verification-code.ttl-seconds:300}") long ttlSeconds,
            @Value("${auth.verification-code.expose-in-response:true}") boolean exposeCodeInResponse,
            ObjectProvider<SmsSender> smsSenderProvider) {
        this.ttl = Duration.ofSeconds(Math.max(60, ttlSeconds));
        this.exposeCodeInResponse = exposeCodeInResponse;
        this.smsSender = smsSenderProvider.getIfAvailable();
    }

    @Override
    public VerificationCodeDTO sendPhoneCode(String phone) {
        String normalizedPhone = normalizePhone(phone);
        CodeEntry existing = codeStore.get(normalizedPhone);
        Instant now = Instant.now();
        if (existing != null && Duration.between(existing.sentAt(), now).compareTo(SEND_COOLDOWN) < 0) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "Verification code sent too frequently");
        }

        String code = String.format("%06d", RANDOM.nextInt(CODE_LENGTH_BOUND));
        Instant expiresAt = now.plus(ttl);
        if (smsSender != null) {
            smsSender.sendVerificationCode(normalizedPhone, code);
        } else {
            log.info("Mock phone verification code for {} is {}, expires at {}", normalizedPhone, code, expiresAt);
        }
        codeStore.put(normalizedPhone, new CodeEntry(code, now, expiresAt));
        return new VerificationCodeDTO(normalizedPhone, Math.toIntExact(ttl.toSeconds()),
                exposeCodeInResponse ? code : null);
    }

    @Override
    public void verifyPhoneCode(String phone, String code) {
        String normalizedPhone = normalizePhone(phone);
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Verification code is required");
        }

        CodeEntry entry = codeStore.get(normalizedPhone);
        if (entry == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Verification code not found or expired");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            codeStore.remove(normalizedPhone);
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Verification code has expired");
        }
        if (!entry.code().equals(code.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid verification code");
        }
        codeStore.remove(normalizedPhone);
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Phone cannot be blank");
        }
        String normalized = phone.trim();
        if (!normalized.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid phone number format");
        }
        return normalized;
    }

    private record CodeEntry(String code, Instant sentAt, Instant expiresAt) {
    }
}
