package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class DiscussionCommentSaveDTO {
    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;

    private Long parentCommentId;
}
