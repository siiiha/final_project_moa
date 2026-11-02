package com.nightstudy.moa.report.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.report.dto.MemberReportRecord;
import com.nightstudy.moa.report.dto.ReportCaseRecord;
import com.nightstudy.moa.report.dto.ReportCreateRequest;
import com.nightstudy.moa.report.dto.ReportTarget;
import com.nightstudy.moa.report.mapper.ReportMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 신고 대상·본인·중복 여부를 검증하고 사건과 개별 신고를 한 트랜잭션으로 저장한다.
public class ReportServiceImpl implements ReportService {
    private final ReportMapper reportMapper;

    public ReportServiceImpl(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    @Transactional
    public Long createReport(Long reporterId, ReportCreateRequest request) {
        ReportTarget target = reportMapper.selectReportTarget(
                request.getReportType(), request.getTargetId());
        if (target == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORT_TARGET_NOT_FOUND",
                    "신고 대상을 찾을 수 없습니다."
            );
        }
        if (reporterId.equals(target.getReportedMemberId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SELF_REPORT_NOT_ALLOWED",
                    "본인이 작성했거나 본인에 해당하는 대상은 신고할 수 없습니다."
            );
        }
        if (reportMapper.countReporterTarget(
                reporterId, request.getReportType(), request.getTargetId()) > 0) {
            throw duplicateReportException();
        }

        Long caseId = reportMapper.selectPendingCaseId(
                request.getReportType(), request.getTargetId());
        if (caseId == null) {
            ReportCaseRecord reportCase = new ReportCaseRecord(
                    request.getReportType(),
                    request.getTargetId(),
                    target.getReportedMemberId()
            );
            if (reportMapper.insertReportCase(reportCase) != 1
                    || reportCase.getCaseId() == null) {
                throw saveFailedException();
            }
            caseId = reportCase.getCaseId();
        }

        MemberReportRecord memberReport = new MemberReportRecord(
                caseId, reporterId, request.getReason());
        try {
            if (reportMapper.insertMemberReport(memberReport) != 1
                    || memberReport.getReportId() == null) {
                throw saveFailedException();
            }
        } catch (DuplicateKeyException e) {
            throw duplicateReportException();
        }
        return memberReport.getReportId();
    }

    private ApiException duplicateReportException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "DUPLICATE_REPORT",
                "이미 신고한 대상입니다."
        );
    }

    private ApiException saveFailedException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "REPORT_SAVE_FAILED",
                "신고 상태가 변경되어 접수하지 못했습니다."
        );
    }
}
