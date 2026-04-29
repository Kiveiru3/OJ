package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class ChangePasswordDTO {
    @NotBlank(message = "Old password cannot be blank")
    private String oldPassword;

    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
}
