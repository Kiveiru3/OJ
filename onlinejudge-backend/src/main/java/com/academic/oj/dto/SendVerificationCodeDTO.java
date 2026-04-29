package com.academic.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendVerificationCodeDTO {
    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;
}
