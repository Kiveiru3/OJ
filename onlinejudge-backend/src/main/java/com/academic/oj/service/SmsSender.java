package com.academic.oj.service;

import com.academic.oj.dto.VerificationCodeDTO;

public interface SmsSender {
    void sendVerificationCode(String phone, String code);

    default boolean managesVerification() {
        return false;
    }

    default VerificationCodeDTO sendManagedVerificationCode(String phone) {
        throw new UnsupportedOperationException("Managed verification is not supported");
    }

    default void verifyManagedCode(String phone, String code) {
        throw new UnsupportedOperationException("Managed verification is not supported");
    }
}
