package com.nightstudy.moa.notification.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private Long notificationId;
    private NotificationType type;
    private String message;
    private String targetUrl;
    private boolean read;
    private LocalDateTime createdAt;

    // AI작성: DB 조회용 DTO를 개인정보가 제외된 API 응답으로 변환합니다.
    public static NotificationResponse from(NotificationDto notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getMessage(),
                notification.getTargetUrl(),
                notification.getReadAt() != null,
                // 읽은 시간이 있으면 true
                // 읽은 시간이 없으면 false
                notification.getCreatedAt()
        );
    } // from 메서드 종료
}     // NotificationResponse 클래스 종료