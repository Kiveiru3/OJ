package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.AiChatRequestDTO;
import com.academic.oj.dto.AiChatResponseDTO;
import com.academic.oj.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public Result<AiChatResponseDTO> chat(@Validated @RequestBody AiChatRequestDTO requestDTO) {
        String content = aiService.chat(requestDTO.getMessage(), requestDTO.getScene());
        return Result.success(new AiChatResponseDTO(content));
    }
}
