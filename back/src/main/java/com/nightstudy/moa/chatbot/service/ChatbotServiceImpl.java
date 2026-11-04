package com.nightstudy.moa.chatbot.service;

import com.nightstudy.moa.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatClient chatClient;
    private final boolean enabled;

    public ChatbotServiceImpl(
            ChatClient.Builder builder,
            @Value("classpath:prompts/chatbot-system.st") Resource systemPrompt,
            @Value("${spring.ai.openai.api-key:no-key}") String apiKey
    ) {
        this.chatClient = builder
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .build();
        this.enabled = !apiKey.isBlank() && !"no-key".equals(apiKey);
    }

    @Override
    public String answer(String message) {
        if (!enabled) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CHATBOT_UNAVAILABLE",
                    "지금은 모아 챗봇을 이용할 수 없어요."
            );
        }

        String reply = chatClient.prompt()
                .user(message)
                .call()
                .content();

        if (reply == null || reply.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "CHATBOT_EMPTY_REPLY",
                    "답변을 받지 못했어요. 잠시 후 다시 시도해 주세요."
            );
        }

        return reply;
    }
}