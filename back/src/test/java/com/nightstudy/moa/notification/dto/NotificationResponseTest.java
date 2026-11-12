package com.nightstudy.moa.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

// AI작성: UTC로 저장된 알림 생성 시간이 API 응답에서 한국 표준시로 변환되는지 확인한다.
class NotificationResponseTest {

    @Test
    void from_convertsUtcDatabaseTimeToKoreaStandardTime() {
        NotificationDto notification = new NotificationDto();
        notification.setNotificationId(1L);
        notification.setType(NotificationType.NOTICE_CREATED);
        notification.setMessage("새 공지입니다.");
        notification.setCreatedAt(LocalDateTime.of(2026, 11, 12, 1, 30));

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.getCreatedAt()).isEqualTo(
                OffsetDateTime.of(2026, 11, 12, 10, 30, 0, 0, ZoneOffset.ofHours(9))
        );
    }
}
