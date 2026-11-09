package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SavingGroupDuesNotificationScheduler {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String ADVANCE_REMINDER = "ADVANCE";
    private static final String DUE_TODAY_REMINDER = "DUE_TODAY";

    private final SavingGroupMapper savingGroupMapper;
    private final NotificationService notificationService;

    public SavingGroupDuesNotificationScheduler(
            SavingGroupMapper savingGroupMapper,
            NotificationService notificationService) {
        this.savingGroupMapper = savingGroupMapper;
        this.notificationService = notificationService;
    }

    // AI작성: 한국 시간 기준 납부일 3일 전과 당일 오전 9시에 각 알림 대상을 확인한다.
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDuesPaymentReminders() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        sendReminder(today.plusDays(3), ADVANCE_REMINDER, "회비 납부일이 3일 남았습니다.");
        sendReminder(today, DUE_TODAY_REMINDER, "오늘은 회비 납부일입니다.");
    }

    private void sendReminder(LocalDate paymentDate, String reminderKey, String messagePrefix) {
        YearMonth paymentMonth = YearMonth.from(paymentDate);
        var start = paymentMonth.atDay(1).atStartOfDay();
        var end = paymentMonth.plusMonths(1).atDay(1).atStartOfDay();
        String paymentMonthValue = paymentMonth.toString();

        savingGroupMapper.selectDuesNotificationTargets(
                        paymentDate.getDayOfMonth(), start, end, paymentMonthValue, reminderKey)
                .forEach(target -> {
                    String targetUrl = "/saving-groups/" + target.savingGroupId()
                            + "?menu=dues&paymentMonth=" + paymentMonthValue
                            + "&reminder=" + reminderKey;
                    // AI작성: 회비 안내에 금액과 원화 단위를 명확히 표시한다.
                    String message = messagePrefix + " 납부 금액: "
                            + NumberFormat.getNumberInstance(Locale.KOREA).format(target.duesAmount()) + "원";
                    notificationService.send(
                            target.receiverId(),
                            NotificationType.DUES_PAYMENT_DUE,
                            message,
                            targetUrl
                    );
                });
    }
}
