package com.nightstudy.moa.notification.service;

import com.nightstudy.moa.notification.dto.CommentNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentNotificationEventListener {

    private final NotificationService notificationService;

    // AI작성: 댓글 저장이 확정된 뒤 별도 트랜잭션에서 알림을 저장하고 커밋 후 SSE로 전송합니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CommentNotificationEvent event) {
        notificationService.send(
                event.receiverId(),
                event.type(),
                event.message(),
                event.targetUrl()
        );
    }
}
