package com.nightstudy.moa.notification.dto;

// AI작성: 댓글 트랜잭션과 알림 처리를 분리하기 위해 커밋 후 전달할 값만 보관합니다.
public record CommentNotificationEvent(
        Long receiverId,
        NotificationType type,
        String message,
        String targetUrl
) {
}
