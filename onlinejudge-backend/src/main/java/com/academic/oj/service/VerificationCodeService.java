package com.academic.oj.service;

import com.academic.oj.dto.VerificationCodeDTO;

public interface VerificationCodeService {
    VerificationCodeDTO sendPhoneCode(String phone);

    void verifyPhoneCode(String phone, String code);
}
