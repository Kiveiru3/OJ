package com.academic.oj.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ProblemBatchImportDTO {
    @NotEmpty(message = "problems cannot be empty")
    @Valid
    private List<ProblemImportItemDTO> problems;

    private Boolean skipExistingTitle = true;
}

