package com.nightstudy.moa.notification.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.notification.dto.NotificationResponse;
import com.nightstudy.moa.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    // AI작성: 알림 목록 요청의 기본값과 허용 범위를 한 곳에서 관리합니다.
    private static final String DEFAULT_LIST_SIZE = "20";
    private static final int MIN_LIST_SIZE = 1;
    private static final int MAX_LIST_SIZE = 50;

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
            @RequestParam(defaultValue = DEFAULT_LIST_SIZE) int size
    ) {
        int normalizedSize = Math.min(
                Math.max(size, MIN_LIST_SIZE),
                MAX_LIST_SIZE
        );

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

    // AI작성: 인증된 회원 본인의 읽은 알림만 삭제하고 삭제 건수를 반환합니다.
    @DeleteMapping("/read")
    public ApiResponse<Integer> deleteAllRead(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(
                notificationService.deleteAllRead(memberId)
        );
    }

}
