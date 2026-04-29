package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class TestCaseDTO {
    private Long id;

    @NotNull(message = "Input cannot be null")
    private String input;

    @NotNull(message = "Output cannot be null")
    private String output;
}
