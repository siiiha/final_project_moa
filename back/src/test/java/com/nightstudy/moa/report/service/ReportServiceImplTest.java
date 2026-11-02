package com.nightstudy.moa.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.report.dto.MemberReportRecord;
import com.nightstudy.moa.report.dto.ReportCaseRecord;
import com.nightstudy.moa.report.dto.ReportStatus;
import com.nightstudy.moa.report.dto.ReportCreateRequest;
import com.nightstudy.moa.report.dto.ReportTarget;
import com.nightstudy.moa.report.dto.ReportType;
import com.nightstudy.moa.report.mapper.ReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 회원 신고의 정상 저장과 대상·본인·중복 검증 규칙을 확인한다.
class ReportServiceImplTest {
    @Mock
    private ReportMapper reportMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void createReportCreatesCaseAndSubmission() {
        ReportCreateRequest request = request(ReportType.POST, 10L);
        when(reportMapper.selectReportTarget(ReportType.POST, 10L))
                .thenReturn(target(2L));
        when(reportMapper.selectPendingCaseId(
                ReportType.POST, 10L, ReportStatus.PENDING)).thenReturn(null);
        when(reportMapper.insertReportCase(any(ReportCaseRecord.class))).thenAnswer(invocation -> {
            ReportCaseRecord reportCase = invocation.getArgument(0);
            reportCase.setCaseId(100L);
            return 1;
        });
        when(reportMapper.insertMemberReport(any(MemberReportRecord.class))).thenAnswer(invocation -> {
            MemberReportRecord memberReport = invocation.getArgument(0);
            memberReport.setReportId(200L);
            return 1;
        });

        Long reportId = reportService.createReport(1L, request);

        assertThat(reportId).isEqualTo(200L);
        verify(reportMapper).insertReportCase(argThat(reportCase ->
                reportCase.getReportType() == ReportType.POST
                        && reportCase.getTargetId().equals(10L)
                        && reportCase.getReportedMemberId().equals(2L)));
        verify(reportMapper).insertMemberReport(argThat(memberReport ->
                memberReport.getCaseId().equals(100L)
                        && memberReport.getReporterId().equals(1L)
                        && memberReport.getReason().equals("광고성 게시글입니다.")));
    }

    @Test
    void createReportRejectsMissingTarget() {
        ReportCreateRequest request = request(ReportType.COMMENT, 10L);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("REPORT_TARGET_NOT_FOUND"));
        verify(reportMapper, never()).insertMemberReport(any(MemberReportRecord.class));
    }

    @Test
    void createReportRejectsSelfReport() {
        ReportCreateRequest request = request(ReportType.MEMBER, 1L);
        when(reportMapper.selectReportTarget(ReportType.MEMBER, 1L))
                .thenReturn(target(1L));

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("SELF_REPORT_NOT_ALLOWED"));
    }

    @Test
    void createReportRejectsDuplicateReporterTarget() {
        ReportCreateRequest request = request(ReportType.POST, 10L);
        when(reportMapper.selectReportTarget(ReportType.POST, 10L))
                .thenReturn(target(2L));
        when(reportMapper.countReporterTarget(1L, ReportType.POST, 10L)).thenReturn(1);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("DUPLICATE_REPORT"));
    }

    private ReportCreateRequest request(ReportType reportType, Long targetId) {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setReportType(reportType);
        request.setTargetId(targetId);
        request.setReason("광고성 게시글입니다.");
        return request;
    }

    private ReportTarget target(Long reportedMemberId) {
        ReportTarget target = new ReportTarget();
        target.setReportedMemberId(reportedMemberId);
        return target;
    }
}
