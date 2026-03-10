package com.academic.oj.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProblemBatchImportResultDTO {
    private Integer total;
    private Integer imported;
    private Integer skipped;
    private Integer failed;
    private List<String> errors = new ArrayList<>();
}

