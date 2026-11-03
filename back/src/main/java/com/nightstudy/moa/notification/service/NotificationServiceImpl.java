package com.nightstudy.moa.notification.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.sse.SseEmitters;
import com.nightstudy.moa.notification.dto.NotificationDto;
import com.nightstudy.moa.notification.dto.NotificationResponse;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.mapper.NotificationMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final SseEmitters sseEmitters;

    @Override
    public SseEmitter subscribe(Long receiverId) {
        return sseEmitters.add(receiverId);
    }

    // AI작성: 알림을 DB에 먼저 저장한 후 접속 중인 회원에게 실시간 전송합니다.
    @Override
    public void send(
            Long receiverId,
            NotificationType type,
            String message,
            String targetUrl
    ) {
        NotificationDto notification = new NotificationDto();
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setTargetUrl(targetUrl);

        notificationMapper.insert(notification);

        NotificationDto saved =
                notificationMapper.findById(notification.getNotificationId());

        NotificationResponse response =
                NotificationResponse.from(saved);

        sseEmitters.send(
                receiverId,
                "notification",
                saved.getNotificationId(),
                response
        );
    }

    // AI작성: DB 조회 결과에서 회원 번호를 제외한 응답 객체 목록을 생성합니다.
    @Override
    public List<NotificationResponse> getRecent(
            Long receiverId,
            Long lastNotificationId,
            int size
    ) {
        return notificationMapper
                .findRecent(receiverId, lastNotificationId, size)
                .stream()
                // 목록 조회
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    public int countUnread(Long receiverId) {
        return notificationMapper.countUnread(receiverId);
    }

    // AI작성: 본인의 알림이 존재하면 이미 읽은 알림도 오류 없이 성공 처리합니다.
    @Override
    public void read(Long receiverId, Long notificationId) {
        boolean exists = notificationMapper.existsByIdAndReceiverId(
                notificationId,
                receiverId
        );

        if (!exists) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOTIFICATION_NOT_FOUND",
                    "알림을 찾을 수 없습니다."
            );
        }

        notificationMapper.markRead(notificationId, receiverId);
    }

    @Override
    public void readAll(Long receiverId) {
        notificationMapper.markAllRead(receiverId);
    }

    // AI작성: 회원 본인의 읽은 알림만 삭제하고 삭제된 알림 개수를 호출자에게 전달합니다.
    @Override
    public int deleteAllRead(Long receiverId) {
        return notificationMapper.deleteAllRead(receiverId);
    }
}
