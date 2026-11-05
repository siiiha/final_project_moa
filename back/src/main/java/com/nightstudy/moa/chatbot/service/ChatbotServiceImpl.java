package com.nightstudy.moa.chatbot.service;

import com.nightstudy.moa.chatbot.util.PiiMasker;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    public ChatbotServiceImpl(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            @Value("classpath:prompts/chatbot-system.st") Resource systemPrompt,
            @Value("${spring.ai.openai.api-key:no-key}") String apiKey
    ) {
        this.chatClient = builder
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .build();
        this.chatMemoryAdvisor =
                MessageChatMemoryAdvisor.builder(chatMemory).build();

        this.enabled = !apiKey.isBlank() && !"no-key".equals(apiKey);
    }

    @Override
    public String answer(Long memberId, String message) {
        if (!enabled) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CHATBOT_UNAVAILABLE",
                    "지금은 모아 챗봇을 이용할 수 없어요."
            );
        }

        String maskedMessage = PiiMasker.mask(message);
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt()
                .user(maskedMessage);

        if (memberId != null) {
            // 로그한 회원에게만 회원별 대화 기록 연결
            prompt.advisors(chatMemoryAdvisor)
                    .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID, "member-" +
                    memberId));
        }

        String reply = prompt.call().content();

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