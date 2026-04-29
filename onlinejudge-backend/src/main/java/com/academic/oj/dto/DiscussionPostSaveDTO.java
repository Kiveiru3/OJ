package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class DiscussionPostSaveDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must not exceed 120 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    private Long problemId;
}
