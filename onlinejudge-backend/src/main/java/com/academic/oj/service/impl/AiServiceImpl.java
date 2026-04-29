package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    public AiServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder
                .defaultSystem("""
                        You are an AI assistant inside an Online Judge system.
                        Answer in Chinese by default. Focus on learning guidance, debugging ideas,
                        algorithm intuition, and safe hints. Do not provide full contest solutions
                        unless the user explicitly asks for explanation after solving.
                        """)
                .build();
    }

    @Override
    public String chat(String message, String scene) {
        if (chatClient == null) {
            throw new BusinessException(ResultCode.ERROR.getCode(),
                    "AI model is not configured. Set SPRING_AI_MODEL_CHAT=openai and OPENAI_API_KEY first.");
        }
        String prompt = buildPrompt(message, scene);
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("AI chat request failed", ex);
            throw new BusinessException(ResultCode.ERROR.getCode(), "AI request failed: " + ex.getMessage());
        }
    }

    private String buildPrompt(String message, String scene) {
        String trimmedMessage = message == null ? "" : message.trim();
        if (!StringUtils.hasText(scene)) {
            return trimmedMessage;
        }
        return "Scene: " + scene.trim() + "\n\nUser message:\n" + trimmedMessage;
    }
}
