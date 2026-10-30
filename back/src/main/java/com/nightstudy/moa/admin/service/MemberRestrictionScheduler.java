package com.nightstudy.moa.admin.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
// 설정 클래스 : Spring 애플리케이션이 어떻게 동작할지 설정하는 클래스
@EnableScheduling
// 예약된 작업 실행 기능 활성화

// AI작성: 기간이 끝난 회원의 이용 제한을 정기적으로 자동 해제하는 스케줄러이다.
public class MemberRestrictionScheduler {

    private final AdminReportService adminReportService;

    // AI작성: 자동 해제의 실제 DB 처리는 Service 계층에 위임한다.
    public MemberRestrictionScheduler(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    // AI작성: 매시간 정각에 만료된 기간제 이용 제한을 찾아 해제한다.
    // AI작성: restricted_until이 NULL인 기능별 영구 제한은 자동 정리 대상에 포함하지 않는다.
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void releaseExpiredRestrictions() {
        adminReportService.releaseExpiredMemberRestrictions();
    }
}
