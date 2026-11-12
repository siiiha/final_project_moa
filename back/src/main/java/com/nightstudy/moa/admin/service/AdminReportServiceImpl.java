package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.admin.dto.AdminReportDetailResponse;
import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionResponse;
import com.nightstudy.moa.admin.dto.AdminReportTargetResponse;
import com.nightstudy.moa.admin.dto.AdminReportProcessRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import com.nightstudy.moa.admin.dto.RestrictionAction;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.admin.mapper.AdminReportMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
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
    private final AdminMemberMapper adminMemberMapper;
    private final MemberRestrictionPolicy memberRestrictionPolicy;
    private final NotificationService notificationService;

    // AI작성: 신고 조회와 상태 변경에 사용할 Mapper를 생성자 주입으로 전달받는다.
    @Autowired
    public AdminReportServiceImpl(
            AdminReportMapper adminReportMapper,
            AdminMemberMapper adminMemberMapper,
            MemberRestrictionPolicy memberRestrictionPolicy,
            NotificationService notificationService
    ){
        this.adminReportMapper = adminReportMapper;
        this.adminMemberMapper = adminMemberMapper;
        this.memberRestrictionPolicy = memberRestrictionPolicy;
        this.notificationService = notificationService;
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
        // AI작성: 회원 신고는 대상 콘텐츠가 없으므로 null을, 게시글·댓글 신고는 실제 원문을 반환합니다.
        AdminReportTargetResponse targetContent =
                adminReportMapper.selectReportTargetContent(caseId);

        AdminReportDetailResponse result = new AdminReportDetailResponse(
                reportCase,
                targetContent,
                submissions
        );
        return result;
    }

    @Override
    @Transactional
    public void processReport(Long reportId, AdminReportProcessRequest request) {
        // AI작성: 같은 신고의 동시·중복 처리를 막기 위해 신고 행을 먼저 잠근다.
        AdminReportListItemResponse report = adminReportMapper.selectReportForUpdate(reportId);
        if (report == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORT_NOT_FOUND",
                    "신고를 찾을 수 없습니다."
            );
        }

        if (!"PENDING".equals(report.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REPORT_ALREADY_PROCESSED",
                    "이미 처리된 신고입니다."
            );
        }

        validateProcessRequest(report.getReportType(), request);
        validateContentDeletionRequest(report.getReportType(), request);

        // AI작성: 반려는 신고 상태와 사유만 변경하고, 정상 처리일 때만 회원을 이용 제한한다.
        LocalDateTime restrictedUntil = null;
        if ("CONFIRMED".equals(request.getStatus()) && request.getRestrictionScope() != null) {
            restrictedUntil = applyMemberRestriction(
                    reportId,
                    request.getRestrictionScope(),
                    request.getRestrictionType(),
                    request.getProcessReason()
            );
            deleteReportedContentIfRequested(reportId, report.getReportType(), request.getDeleteTargetContent());
        }

        int updatedCount = adminReportMapper.updateReportProcess(
                reportId,
                request.getStatus(),
                request.getProcessReason(),
                restrictedUntil
        );

        if (updatedCount != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REPORT_PROCESS_FAILED",
                    "신고 처리 상태가 변경되어 처리하지 못했습니다."
            );
        }

        // AI작성: 사건 처리가 확정된 뒤 같은 사건을 접수한 모든 신고자에게 승인·반려 결과를 알린다.
        String notificationMessage = "CONFIRMED".equals(request.getStatus())
                ? "신고하신 내용이 확인되어 처리되었습니다."
                : "신고하신 내용을 검토했으나 반려되었습니다.";
        adminReportMapper.selectReportReporterIds(reportId)
                .forEach(reporterId -> notificationService.send(
                        reporterId,
                        NotificationType.REPORT_PROCESSED,
                        notificationMessage,
                        null
                ));
    }

    @Override
    @Transactional
    // AI작성: 승인된 게시글·댓글 신고의 대상만 잠금 확인 후 후속 삭제하며, 임의 콘텐츠 삭제를 막는다.
    public void deleteApprovedReportedContent(Long reportId) {
        AdminReportListItemResponse report = adminReportMapper.selectReportForUpdate(reportId);
        if (report == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "신고를 찾을 수 없습니다.");
        }
        if (!"CONFIRMED".equals(report.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REPORT_CONTENT_DELETE_REQUIRES_CONFIRMATION",
                    "승인된 신고의 콘텐츠만 삭제할 수 있습니다.");
        }

        int deletedCount = switch (report.getReportType()) {
            case "POST" -> adminReportMapper.deleteReportedPost(reportId);
            case "COMMENT" -> adminReportMapper.deleteReportedComment(reportId);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_CONTENT_DELETE_NOT_ALLOWED",
                    "게시글 또는 댓글 신고의 콘텐츠만 삭제할 수 있습니다.");
        };
        if (deletedCount != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "REPORT_CONTENT_ALREADY_DELETED",
                    "이미 삭제되었거나 삭제할 수 없는 신고 콘텐츠입니다.");
        }
    }

    @Override
    @Transactional
    // AI작성: 스케줄러가 호출하면 만료된 기간제 제한을 일괄 해제하고 처리 건수를 반환한다.
    // 서비스 단위의 DB변경이라 @Transactional을 붙였는데 UPDATE는 한 번만 일어나서... 삭제해도 무방합니다.
    public int releaseExpiredMemberRestrictions() {
        return adminMemberMapper.deleteExpiredMemberRestrictions();
    }

    // AI작성: 처리 결과와 이용 제한 값이 서로 맞는 조합인지 검증한다.
    private void validateProcessRequest(String reportType, AdminReportProcessRequest request) {
        boolean confirmed = "CONFIRMED".equals(request.getStatus());
        boolean hasRestrictionType = request.getRestrictionType() != null;
        boolean hasRestrictionScope = request.getRestrictionScope() != null;
        MemberRestrictionScope allowedScope = getAllowedRestrictionScope(reportType);

        if (confirmed && allowedScope != null && (!hasRestrictionType || !hasRestrictionScope)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RESTRICTION_REQUIRED",
                    "신고를 승인하려면 이용 제한 범위와 기간을 선택해야 합니다."
            );
        }

        if (!confirmed && (hasRestrictionType || hasRestrictionScope)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RESTRICTION_NOT_ALLOWED",
                    "반려 처리에는 이용 제한 범위와 기간을 입력할 수 없습니다."
            );
        }

        if (confirmed && allowedScope == null && (hasRestrictionType || hasRestrictionScope)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_TYPE_RESTRICTION_NOT_ALLOWED",
                    "회원 또는 모임 신고에는 이용 제한을 적용할 수 없습니다.");
        }

        if (confirmed && allowedScope != null && request.getRestrictionScope() != allowedScope) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_TYPE_RESTRICTION_SCOPE_MISMATCH",
                    "신고 유형에 맞는 이용 제한 영역만 선택할 수 있습니다.");
        }
    }

    // AI작성: 신고 유형별로 관련된 이용 기능만 제한하도록 고정 매핑한다.
    private MemberRestrictionScope getAllowedRestrictionScope(String reportType) {
        return switch (reportType) {
            case "POST", "COMMENT" -> MemberRestrictionScope.CONTENT;
            case "CHAT_MESSAGE" -> MemberRestrictionScope.CHAT;
            default -> null;
        };
    }

    // AI작성: 콘텐츠 삭제는 승인된 게시글·댓글 신고에서만 선택할 수 있도록 제한합니다.
    private void validateContentDeletionRequest(String reportType, AdminReportProcessRequest request) {
        if (!Boolean.TRUE.equals(request.getDeleteTargetContent())) {
            return;
        }
        if (!"CONFIRMED".equals(request.getStatus())
                || !("POST".equals(reportType) || "COMMENT".equals(reportType))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REPORT_CONTENT_DELETE_NOT_ALLOWED",
                    "게시글 또는 댓글 신고를 승인할 때만 신고 콘텐츠를 삭제할 수 있습니다."
            );
        }
    }

    // AI작성: 이미 삭제된 콘텐츠는 신고 처리를 막지 않고, 공개 중인 대상만 소프트 삭제합니다.
    private void deleteReportedContentIfRequested(Long reportId, String reportType, Boolean deleteTargetContent) {
        if (!Boolean.TRUE.equals(deleteTargetContent)) {
            return;
        }
        if ("POST".equals(reportType)) {
            adminReportMapper.deleteReportedPost(reportId);
        } else if ("COMMENT".equals(reportType)) {
            adminReportMapper.deleteReportedComment(reportId);
        }
    }

    // AI작성: 신고 사건에 기록된 제재 대상 회원에게 이용 제한을 적용한다.
    private LocalDateTime applyMemberRestriction(
            Long reportId,
            MemberRestrictionScope restrictionScope,
            String restrictionType,
            String reason
    ) {
        Long memberId = adminReportMapper.selectReportedMemberId(reportId);
        if (memberId == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORTED_MEMBER_NOT_FOUND",
                    "신고 대상 회원을 찾을 수 없습니다."
            );
        }

        AdminMemberRestrictionTarget target = adminMemberMapper.selectRestrictionTargetForUpdate(
                memberId,
                restrictionScope
        );
        if (target == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원을 찾을 수 없습니다."
            );
        }

        if ("INACTIVE".equals(target.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INACTIVE_MEMBER_RESTRICTION_NOT_ALLOWED",
                    "비활성 회원은 이용 제한할 수 없습니다."
            );
        }

        // AI작성: 관리자가 선택한 기능 범위의 기존 제한 종료 시각부터 새 제한 기간을 누적한다.
        LocalDateTime restrictedUntil = memberRestrictionPolicy.calculateRestrictedUntil(
                target.isCurrentlyRestricted(),
                target.getRestrictedUntil(),
                restrictionType,
                LocalDateTime.now()
        );

        adminMemberMapper.upsertMemberRestriction(memberId, restrictionScope, restrictedUntil);
        if (adminMemberMapper.insertRestrictionHistory(
                memberId,
                RestrictionAction.RESTRICT,
                restrictionType,
                restrictionScope,
                reason,
                restrictedUntil
        ) != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MEMBER_RESTRICTION_FAILED",
                    "회원 이용 제한을 적용하지 못했습니다."
            );
        }
        return restrictedUntil;
    }

}
