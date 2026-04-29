package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SystemConfigUpdateDTO {
    @NotBlank(message = "Config key is required")
    @Size(max = 100, message = "Config key must not exceed 100 characters")
    private String configKey;

    @Size(max = 5000, message = "Config value must not exceed 5000 characters")
    private String configValue;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
