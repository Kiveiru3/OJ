package com.academic.oj.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AdminUserUpdateDTO {
    @NotBlank(message = "Role is required")
    private String role;

    @NotNull(message = "Status is required")
    private Integer status;
}
