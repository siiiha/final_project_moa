package com.nightstudy.moa.notification.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.notification.dto.NotificationResponse;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    // AI작성: SSE 응답은 JSON 한 건이 아닌 이벤트 흐름이므로 ApiResponse로 감싸지 않습니다.
    @GetMapping(
            value = "/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribe(
            @AuthenticationPrincipal Long memberId
    ) {
        return notificationService.subscribe(memberId);
    }

    // AI작성: 마지막으로 받은 알림 번호가 있으면 그 이전 알림을 조회합니다.
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long lastNotificationId,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 50);

        return ApiResponse.ok(
                notificationService.getRecent(
                        memberId,
                        lastNotificationId,
                        normalizedSize
                )
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(
                notificationService.countUnread(memberId)
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> read(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long notificationId
    ) {
        notificationService.read(memberId, notificationId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> readAll(
            @AuthenticationPrincipal Long memberId
    ) {
        notificationService.readAll(memberId);
        return ApiResponse.ok(null);
    }

    // 확인용 — 발표 전 삭제하거나 개발 환경에서만 사용해야 합니다.
    @PostMapping("/test")
    public ApiResponse<Void> test(
            @AuthenticationPrincipal Long memberId
    ) {
        notificationService.send(
                memberId,
                NotificationType.TEST,
                "테스트 알림입니다.",
                "/mypage"
        );

        return ApiResponse.ok(null);
    }
}