package com.nightstudy.moa.notification.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private Long notificationId;
    private NotificationType type;
    private String message;
    private String targetUrl;
    private boolean read;
    private OffsetDateTime createdAt;

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
                // AI작성: UTC로 저장된 DATETIME을 실제 시점으로 변환한 뒤 한국 표준시 오프셋을 포함해 응답한다.
                notification.getCreatedAt() == null
                        ? null
                        : notification.getCreatedAt()
                                .atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant(KOREA_ZONE_ID)
                                .toOffsetDateTime()
        );
    } // from 메서드 종료
}     // NotificationResponse 클래스 종료
