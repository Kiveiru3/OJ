package com.academic.oj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerificationCodeDTO {
    private String phone;
    private Integer expiresInSeconds;
    private String code;
}
