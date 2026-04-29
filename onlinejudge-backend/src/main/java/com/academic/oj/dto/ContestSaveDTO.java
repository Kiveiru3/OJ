package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    private LocalDateTime scoreboardFreezeTime;

    @NotEmpty(message = "At least one problem is required")
    private List<Long> problemIds;

    private Integer status; // 1-public, 0-hidden
    private Integer penaltyPerWrong;
}
