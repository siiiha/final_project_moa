package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import com.nightstudy.moa.savingGroup.service.SavingGroupChatService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatRoomResponse;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SavingGroupChatController {
    private final SavingGroupChatService chatService;
    private final SimpMessagingTemplate messaging;

    @GetMapping("/api/saving-groups/{savingGroupId}/chat-room")
    public ApiResponse<SavingGroupChatRoomResponse> room(@PathVariable long savingGroupId, @AuthenticationPrincipal Long memberId) {
        return ApiResponse.ok(chatService.findRoom(savingGroupId, memberId));
    }

    @GetMapping("/api/v1/chat/rooms/{roomId}/messages")
    public ApiResponse<List<SavingGroupChatMessage>> history(@PathVariable long roomId,
            @AuthenticationPrincipal Long memberId, @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(chatService.history(roomId, memberId, beforeId, size));
    }

    // AI작성: 저장 트랜잭션이 끝난 뒤에만 구독자에게 메시지를 전달한다.
    @MessageMapping("/chat/rooms/{roomId}")
    public void send(@DestinationVariable long roomId, @Payload SendRequest request, Principal principal) {
        try {
            SavingGroupChatMessage message = chatService.save(roomId, Long.valueOf(principal.getName()), request.content());
            messaging.convertAndSend("/topic/chat/rooms/" + roomId, message);
        } catch (ApiException exception) {
            // AI작성: 도배 제한도 기존 개인 오류 큐로 전달하며 제한 시간은 채팅방 조회로 확인한다.
            messaging.convertAndSendToUser(principal.getName(), "/queue/errors",
                    ApiResponse.fail(exception.getCode(), exception.getMessage()));
        }
    }

    public record SendRequest(String content) {}
}
