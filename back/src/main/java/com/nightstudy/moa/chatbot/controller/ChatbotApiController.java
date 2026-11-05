package com.nightstudy.moa.chatbot.controller;

import com.nightstudy.moa.chatbot.dto.ChatbotReply;
import com.nightstudy.moa.chatbot.dto.ChatbotRequest;
import com.nightstudy.moa.chatbot.service.ChatbotService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private final ChatbotService chatbotService;
    private final GuestChatbotRateLimiter guestChatbotRateLimiter;

    public ChatbotApiController(
            ChatbotService chatbotService,
            GuestChatbotRateLimiter guestChabotRateLimiter
    ) {
        this.chatbotService = chatbotService;
        this.guestChatbotRateLimiter = guestChabotRateLimiter;
    }

    @PostMapping("/messages")
    public ApiResponse<ChatbotReply> sendMessage(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChatbotRequest request,
            HttpServletRequest servletRequest
    ) {
        if (memberId == null) {
            guestChatbotRateLimiter.check(servletRequest.getRemoteAddr());
        }

        String reply = chatbotService.answer(memberId,request.message());
        return ApiResponse.ok(new ChatbotReply(reply));
    }
}