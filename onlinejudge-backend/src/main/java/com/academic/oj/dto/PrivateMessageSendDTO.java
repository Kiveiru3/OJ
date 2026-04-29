package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class PrivateMessageSendDTO {
    @NotNull(message = "toUserId is required")
    private Long toUserId;

    @NotBlank(message = "content is required")
    @Size(max = 2000, message = "content must not exceed 2000 characters")
    private String content;
}
