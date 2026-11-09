package com.nightstudy.moa.notification.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.sse.SseEmitters;
import com.nightstudy.moa.notification.dto.NotificationDto;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.mapper.NotificationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
// AI작성: SSE 최초 연결과 마지막 이벤트 번호를 사용한 재연결 복구 동작을 검증합니다.
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private SseEmitters sseEmitters;

    @Mock
    private SseEmitter emitter;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void subscribe_replaysNotificationsAfterLastEventIdInOrder() {
        Long receiverId = 7L;
        Long lastEventId = 10L;
        NotificationDto first = notification(11L, "첫 번째 누락 알림");
        NotificationDto second = notification(12L, "두 번째 누락 알림");

        when(sseEmitters.add(receiverId)).thenReturn(emitter);
        when(notificationMapper.findAfter(receiverId, lastEventId))
                .thenReturn(List.of(first, second));

        SseEmitter result = notificationService.subscribe(receiverId, lastEventId);

        verify(notificationMapper).findAfter(receiverId, lastEventId);
        InOrder order = inOrder(sseEmitters);
        order.verify(sseEmitters).send(
                receiverId,
                emitter,
                "notification",
                11L,
                argThat(
                        response -> response.getNotificationId().equals(11L)
                )
        );
        order.verify(sseEmitters).send(
                receiverId,
                emitter,
                "notification",
                12L,
                argThat(
                        response -> response.getNotificationId().equals(12L)
                )
        );
        assertSame(emitter, result);
    }

    @Test
    void subscribe_withoutLastEventIdDoesNotQueryRecoveryNotifications() {
        Long receiverId = 7L;
        when(sseEmitters.add(receiverId)).thenReturn(emitter);

        SseEmitter result = notificationService.subscribe(receiverId, null);

        verify(notificationMapper, never()).findAfter(
                anyLong(),
                anyLong()
        );
        assertSame(emitter, result);
    }

    // AI작성: 복구 전송 순서를 확인하는 데 필요한 알림 데이터만 생성합니다.
    private NotificationDto notification(Long notificationId, String message) {
        NotificationDto notification = new NotificationDto();
        notification.setNotificationId(notificationId);
        notification.setReceiverId(7L);
        notification.setType(NotificationType.POST_COMMENT_CREATED);
        notification.setMessage(message);
        notification.setTargetUrl("/post/1");
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
