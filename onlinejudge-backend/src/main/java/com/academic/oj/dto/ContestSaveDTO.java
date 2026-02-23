package com.academic.oj.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestSaveDTO {
    @NotBlank(message = "Contest title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotEmpty(message = "At least one problem is required")
    private List<Long> problemIds;

    private Integer status; // 1-public, 0-hidden
}

