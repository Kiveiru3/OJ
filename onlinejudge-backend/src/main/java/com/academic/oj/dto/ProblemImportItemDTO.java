package com.academic.oj.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ProblemImportItemDTO {
    @NotBlank(message = "title is required")
    private String title;

    private String description;
    private String inputFormat;
    private String outputFormat;
    private String sampleInput;
    private String sampleOutput;
    private String hint;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String difficulty;
    private String tags;
    private Integer status;

    private List<TestCaseDTO> testCases;
}

