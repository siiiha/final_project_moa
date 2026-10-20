package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportDetailResponse;
import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionResponse;
import com.nightstudy.moa.admin.dto.AdminReportProcessRequest;
import com.nightstudy.moa.admin.mapper.AdminReportMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
// AI작성: 관리자 신고 조회, 신고 상태 변경, 대상 회원 이용 제한 규칙을 처리한다.
public class AdminReportServiceImpl implements AdminReportService {
    private final AdminReportMapper adminReportMapper;

    // AI작성: 신고 조회와 상태 변경에 사용할 Mapper를 생성자 주입으로 전달받는다.
    @Autowired
    public AdminReportServiceImpl(AdminReportMapper adminReportMapper){
        this.adminReportMapper = adminReportMapper;
    }

    @Override
    // AI작성: 전체 건수로 페이징 정보를 계산한 후 현재 페이지의 신고 목록을 조회한다.
    public AdminReportListResult getReportList (AdminReportSearchCondition condition){
        int total = adminReportMapper.selectReportCount(condition);
        PageMeta pageMeta = PageMeta.of(
                condition.getPage(),
                condition.getSize(),
                total
        );

        if(total == 0) {
            return new AdminReportListResult(
                    List.of(),
                    pageMeta
            );
        }

        // AI작성: 계산된 시작 위치와 조회 개수를 Mapper의 LIMIT 조건에 전달한다.
        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        List<AdminReportListItemResponse> reports =
                adminReportMapper.selectReportList(condition);
        return new AdminReportListResult(reports, pageMeta);
    }

    @Override
    // AI작성: 신고 사건이 존재하는지 확인한 뒤 사건 정보와 개별 신고 목록을 묶어 반환한다.
    public AdminReportDetailResponse getReportDetail(Long caseId) {
        AdminReportListItemResponse reportCase =
                adminReportMapper.selectReportDetail(caseId);

        if (reportCase == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORT_NOT_FOUND",
                    "신고 사건을 찾을 수 없습니다."
            );
        }

        List<AdminReportSubmissionResponse> submissions =
                adminReportMapper.selectReportSubmissions(caseId);

        AdminReportDetailResponse result = new AdminReportDetailResponse(reportCase, submissions);
        return result;
    }

    @Override
    @Transactional
    public void processReport(Long reportId, AdminReportProcessRequest request) {
        // AI작성: 같은 신고의 동시·중복 처리를 막기 위해 신고 행을 먼저 잠근다.
        String reportStatus = adminReportMapper.selectReportStatusForUpdate(reportId);
        if (reportStatus == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORT_NOT_FOUND",
                    "신고를 찾을 수 없습니다."
            );
        }

        if (!"PENDING".equals(reportStatus)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REPORT_ALREADY_PROCESSED",
                    "이미 처리된 신고입니다."
            );
        }

        validateProcessRequest(request);

        // AI작성: 반려는 신고 상태와 사유만 변경하고, 정상 처리일 때만 회원을 이용 제한한다.
        if ("PROCESSED".equals(request.getStatus())) {
            applyMemberRestriction(reportId, request.getRestrictionType());
        }

        int updatedCount = adminReportMapper.updateReportProcess(
                reportId,
                request.getStatus(),
                request.getProcessReason()
        );

        if (updatedCount != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REPORT_PROCESS_FAILED",
                    "신고 처리 상태가 변경되어 처리하지 못했습니다."
            );
        }
    }

    // AI작성: 처리 결과와 이용 제한 값이 서로 맞는 조합인지 검증한다.
    private void validateProcessRequest(AdminReportProcessRequest request) {
        boolean processed = "PROCESSED".equals(request.getStatus());
        boolean hasRestriction = request.getRestrictionType() != null
                && !request.getRestrictionType().isBlank();

        if (processed && !hasRestriction) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RESTRICTION_REQUIRED",
                    "신고를 처리하려면 이용 제한 값을 선택해야 합니다."
            );
        }

        if (!processed && hasRestriction) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RESTRICTION_NOT_ALLOWED",
                    "반려 처리에는 이용 제한 값을 입력할 수 없습니다."
            );
        }
    }

    // AI작성: USER는 해당 회원, BOARD와 COMMENT는 작성자를 찾아 최종 제재 대상으로 사용한다.
    private void applyMemberRestriction(Long reportId, String restrictionType) {
        Long memberId = adminReportMapper.selectReportedMemberId(reportId);
        if (memberId == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORTED_MEMBER_NOT_FOUND",
                    "신고 대상 회원을 찾을 수 없습니다."
            );
        }

        String accountStatus = adminReportMapper.selectMemberAccountStatus(memberId);
        if (accountStatus == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원을 찾을 수 없습니다."
            );
        }

        if ("INACTIVE".equals(accountStatus)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INACTIVE_MEMBER_RESTRICTION_NOT_ALLOWED",
                    "비활성 회원은 이용 제한할 수 없습니다."
            );
        }

        //새로운 정지 종료 시점을 계산
        LocalDateTime suspendedUntil = calculateSuspendedUntil(
                accountStatus, //현재 회원 계정 상태
                adminReportMapper.selectMemberSuspendedUntil(memberId), //현재 정지 종료 시점
                restrictionType //새로 적용할 이용 제한 유형
        );

        if (adminReportMapper.updateMemberRestriction(memberId, suspendedUntil) != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MEMBER_RESTRICTION_FAILED",
                    "회원 이용 제한을 적용하지 못했습니다."
            );
        }
    }

    // AI작성: 기존 제재보다 짧아지지 않도록 새 종료 시각과 기존 종료 시각을 비교한다.
    private LocalDateTime calculateSuspendedUntil(
            String accountStatus,
            LocalDateTime currentSuspendedUntil,
            String restrictionType
    ) {
        // AI작성: SUSPENDED 상태에서 종료 시각 NULL은 영구 제한을 뜻한다.
        // 영구 정지 요청인가?
        if ("PERMANENT".equals(restrictionType)) {
            return null;
        }

        // 이미 영구 정지된 회원인가?
        if ("SUSPENDED".equals(accountStatus) && currentSuspendedUntil == null) {
            return null;
        }

        // 새로운 정지 종료 시점 계산 (지금 시간 + 정지일)
        LocalDateTime requestedUntil = LocalDateTime.now().plusDays(
                "7_DAYS".equals(restrictionType) ? 7 : 30
        );
        // 기존 종료 시점과 비교
        if (currentSuspendedUntil != null && currentSuspendedUntil.isAfter(requestedUntil)) {
            return currentSuspendedUntil;
        }
        return requestedUntil;
    }
}
