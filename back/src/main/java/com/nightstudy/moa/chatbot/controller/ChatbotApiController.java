package com.nightstudy.moa.chatbot.controller;

import com.nightstudy.moa.chatbot.dto.ChatbotReply;
import com.nightstudy.moa.chatbot.dto.ChatbotRequest;
import com.nightstudy.moa.chatbot.service.ChatbotService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    public ChatbotApiController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/messages")
    public ApiResponse<ChatbotReply> sendMessage(
            @Valid @RequestBody ChatbotRequest request
    ) {
        String reply = chatbotService.answer(request.message());
        return ApiResponse.ok(new ChatbotReply(reply));
    }
}