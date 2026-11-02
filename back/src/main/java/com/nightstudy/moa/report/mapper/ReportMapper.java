package com.nightstudy.moa.report.mapper;

import com.nightstudy.moa.report.dto.ReportTarget;
import com.nightstudy.moa.report.dto.ReportType;
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
            @Param("targetId") Long targetId
    );

    int insertReportCase(
            @Param("reportType") ReportType reportType,
            @Param("targetId") Long targetId,
            @Param("reportedMemberId") Long reportedMemberId
    );

    int insertMemberReport(
            @Param("caseId") Long caseId,
            @Param("reporterId") Long reporterId,
            @Param("reason") String reason
    );

    Long selectLastInsertId();
}
