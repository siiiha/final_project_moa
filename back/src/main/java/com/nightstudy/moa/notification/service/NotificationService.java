package com.nightstudy.moa.notification.service;

import com.nightstudy.moa.notification.dto.NotificationResponse;
import com.nightstudy.moa.notification.dto.NotificationType;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    SseEmitter subscribe(Long receiverId);

    // AI작성: 지정한 회원에게 정해진 종류의 알림을 생성합니다.
    void send(
            Long receiverId,
            NotificationType type,
            String message,
            String targetUrl
    );

    // AI작성: 마지막 알림 번호를 기준으로 이전 알림 목록을 조회합니다.
    // DB용 NotificationDto를 그대로 반환하지 않고, 프론트 전달용 응답 객체를 반환
    List<NotificationResponse> getRecent(
            Long receiverId,
            Long lastNotificationId,
            int size
    );

    int countUnread(Long receiverId);

    void read(Long receiverId, Long notificationId);

    void readAll(Long receiverId);

    // AI작성: 해당 회원이 읽은 알림을 모두 삭제하고 실제 삭제 건수를 반환합니다.
    int deleteAllRead(Long receiverId);
}
