package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SubmitDTO {
    @NotNull(message = "Problem ID cannot be null")
    private Long problemId;
    
    @NotBlank(message = "Code cannot be blank")
    private String code;
    
    @NotBlank(message = "Language cannot be blank")
    private String language;  // JAVA, CPP, PYTHON
}

