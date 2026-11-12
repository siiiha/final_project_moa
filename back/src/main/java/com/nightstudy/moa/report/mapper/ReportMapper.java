package com.nightstudy.moa.report.mapper;

import com.nightstudy.moa.report.dto.MemberReportRecord;
import com.nightstudy.moa.report.dto.ReportCaseRecord;
import com.nightstudy.moa.report.dto.ReportStatus;
import com.nightstudy.moa.report.dto.ReportTarget;
import com.nightstudy.moa.report.dto.ReportType;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 회원 신고 대상 검증과 신고 사건·접수 내역 저장 SQL을 연결한다.
public interface ReportMapper {
    ReportTarget selectReportTarget(
            @Param("reportType") ReportType reportType,
            @Param("targetId") Long targetId
    );

    int countReporterTarget(
            @Param("reporterId") Long reporterId,
            @Param("reportType") ReportType reportType,
            @Param("targetId") Long targetId
    );

    Long selectPendingCaseId(
            @Param("reportType") ReportType reportType,
            @Param("targetId") Long targetId,
            @Param("status") ReportStatus status
    );

    int insertReportCase(ReportCaseRecord reportCase);

    int insertMemberReport(MemberReportRecord memberReport);

    // AI작성: 탈퇴하지 않은 시스템 관리자에게 새 신고 알림을 전송하기 위한 수신자 목록을 조회한다.
    List<Long> selectActiveAdminIds();
}
