package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class AdminResetPasswordDTO {
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 50, message = "Password length must be between 6 and 50")
    private String newPassword;
}
