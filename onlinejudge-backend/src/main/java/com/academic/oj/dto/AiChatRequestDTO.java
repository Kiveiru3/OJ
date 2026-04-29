package com.academic.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequestDTO {
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 4000, message = "Message cannot exceed 4000 characters")
    private String message;

    @Size(max = 120, message = "Scene cannot exceed 120 characters")
    private String scene;
}
