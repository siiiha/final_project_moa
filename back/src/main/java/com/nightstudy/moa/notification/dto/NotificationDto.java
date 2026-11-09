package com.nightstudy.moa.notification.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString

public class NotificationDto {
    private Long notificationId;

    // AI작성: 알림을 받는 회원 번호이며 notification.receiver_id와 연결됩니다.
    private Long receiverId;

    // AI작성: 문자열 오타를 방지하기 위해 알림 종류를 enum으로 관리합니다.
    private NotificationType type;

    private String message;
    private String targetUrl;
    private LocalDateTime readAt;      // null 이면 안 읽음
    private LocalDateTime createdAt;
}