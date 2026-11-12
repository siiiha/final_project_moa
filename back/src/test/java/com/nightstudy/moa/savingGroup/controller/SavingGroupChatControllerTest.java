package com.nightstudy.moa.savingGroup.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.service.restriction.MemberFeatureRestrictionService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupChatMapper;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupChatServiceImpl;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

// AI작성: 실제 채팅 서비스와 Controller를 연결해 저장·방송·개인 오류 응답을 확인한다.
class SavingGroupChatControllerTest {
    @Test
    void reconnectAndDirectSendCannotBypassLimitAndOnlySenderReceivesErrors() {
        var chat = mock(SavingGroupChatMapper.class);
        var groups = mock(SavingGroupMapper.class);
        var restrictions = mock(MemberFeatureRestrictionService.class);
        var messaging = mock(SimpMessagingTemplate.class);
        when(groups.canAccessSavingGroupChat(anyLong(), anyLong())).thenReturn(true);
        when(chat.findRoomId(20L)).thenReturn(10L);
        var stored = new SavingGroupChatMessage();
        stored.setMessageId(1L);
        doAnswer(invocation -> {
            invocation.<SavingGroupChatMessage>getArgument(0).setMessageId(1L);
            return null;
        }).when(chat).insert(any());
        when(chat.findMessage(1L)).thenReturn(stored);
        var controller = new SavingGroupChatController(
                new SavingGroupChatServiceImpl(chat, groups, restrictions), messaging);
        Principal firstConnection = () -> "30";
        for (int i = 0; i < 6; i++) {
            controller.send(10, new SavingGroupChatController.SendRequest("발송 " + i), firstConnection);
        }
        var beforeReconnect = controller.room(20, 30L).getData();
        assertThat(beforeReconnect.spamRestricted()).isTrue();
        // AI작성: Principal 객체가 달라져도 인증된 회원 ID가 같으면 제한을 유지한다.
        Principal reconnected = () -> "30";
        controller.send(10, new SavingGroupChatController.SendRequest("직접 재발송"), reconnected);
        assertThat(controller.room(20, 30L).getData().restrictedUntil()).isEqualTo(beforeReconnect.restrictedUntil());
        verify(chat, times(5)).insert(any());
        verify(messaging, times(5)).convertAndSend("/topic/chat/rooms/10", stored);
        var responses = ArgumentCaptor.forClass(ApiResponse.class);
        verify(messaging, times(2)).convertAndSendToUser(eq("30"), eq("/queue/errors"), responses.capture());
        for (var response : responses.getAllValues()) {
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError().getCode()).isEqualTo("CHAT_RATE_LIMITED");
            assertThat(response.getError().getMessage()).contains("3분");
        }
        controller.send(10, new SavingGroupChatController.SendRequest("다른 회원"), () -> "31");
        controller.send(11, new SavingGroupChatController.SendRequest("다른 방"), reconnected);
        verify(chat, times(7)).insert(any());
        verify(messaging, times(6)).convertAndSend("/topic/chat/rooms/10", stored);
        verify(messaging).convertAndSend("/topic/chat/rooms/11", stored);
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void permissionFailureReturnsPrivateErrorWithoutSavingOrBroadcasting() {
        var chat = mock(SavingGroupChatMapper.class);
        var groups = mock(SavingGroupMapper.class);
        var messaging = mock(SimpMessagingTemplate.class);
        var controller = new SavingGroupChatController(new SavingGroupChatServiceImpl(chat, groups,
                mock(MemberFeatureRestrictionService.class)), messaging);
        controller.send(10, new SavingGroupChatController.SendRequest("권한 없음"), () -> "30");
        var response = ArgumentCaptor.forClass(ApiResponse.class);
        verify(messaging).convertAndSendToUser(eq("30"), eq("/queue/errors"), response.capture());
        assertThat(response.getValue().getError().getCode()).isEqualTo("CHAT_FORBIDDEN");
        verifyNoInteractions(chat);
        verifyNoMoreInteractions(messaging);
    }
}
