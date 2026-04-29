package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AdminUserUpdateDTO {
    @NotBlank(message = "Role is required")
    private String role;

    @NotNull(message = "Status is required")
    private Integer status;
}
