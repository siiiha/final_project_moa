package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportProcessRequest;
import com.nightstudy.moa.admin.dto.RestrictionAction;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.admin.mapper.AdminReportMapper;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 신고 승인 시 관리자가 선택한 기능 제한과 이력이 저장되는지 검증한다.
class AdminReportServiceImplTest {
    @Mock
    private AdminReportMapper adminReportMapper;

    @Mock
    private AdminMemberMapper adminMemberMapper;

    @Mock
    private MemberRestrictionPolicy memberRestrictionPolicy;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    @Test
    void confirmedReportAppliesSelectedRestrictionScopeAndSavesHistory() {
        LocalDateTime restrictedUntil = LocalDateTime.of(2026, 11, 6, 10, 0);
        AdminReportProcessRequest request = new AdminReportProcessRequest();
        request.setStatus("CONFIRMED");
        request.setRestrictionScope(MemberRestrictionScope.CHAT);
        request.setRestrictionType("7_DAYS");
        request.setProcessReason("게시글 운영정책 위반");
        AdminMemberRestrictionTarget target = new AdminMemberRestrictionTarget();
        target.setMemberId(2L);
        target.setAccountStatus("ACTIVE");

        when(adminReportMapper.selectReportForUpdate(10L)).thenReturn(pendingReport("POST"));
        when(adminReportMapper.selectReportedMemberId(10L)).thenReturn(2L);
        when(adminMemberMapper.selectRestrictionTargetForUpdate(2L, MemberRestrictionScope.CHAT))
                .thenReturn(target);
        when(memberRestrictionPolicy.calculateRestrictedUntil(
                eq(false),
                eq(null),
                eq("7_DAYS"),
                any()
        ))
                .thenReturn(restrictedUntil);
        when(adminMemberMapper.insertRestrictionHistory(
                2L,
                RestrictionAction.RESTRICT,
                "7_DAYS",
                MemberRestrictionScope.CHAT,
                "게시글 운영정책 위반",
                restrictedUntil
        )).thenReturn(1);
        when(adminReportMapper.updateReportProcess(
                10L,
                "CONFIRMED",
                "게시글 운영정책 위반",
                restrictedUntil,
                MemberRestrictionScope.CHAT
        )).thenReturn(1);

        adminReportService.processReport(10L, request);

        verify(adminMemberMapper).upsertMemberRestriction(
                2L,
                MemberRestrictionScope.CHAT,
                restrictedUntil
        );
    }

    @Test
    void confirmedReportRequiresRestrictionScope() {
        AdminReportProcessRequest request = new AdminReportProcessRequest();
        request.setStatus("CONFIRMED");
        request.setRestrictionType("7_DAYS");
        request.setProcessReason("운영정책 위반");
        when(adminReportMapper.selectReportForUpdate(10L)).thenReturn(pendingReport("POST"));

        assertThatThrownBy(() -> adminReportService.processReport(10L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("RESTRICTION_REQUIRED"));
    }

    @Test
    void rejectedReportDoesNotAllowRestrictionScope() {
        AdminReportProcessRequest request = new AdminReportProcessRequest();
        request.setStatus("REJECTED");
        request.setRestrictionScope(MemberRestrictionScope.REPORT);
        request.setProcessReason("신고 내용 확인 불가");
        when(adminReportMapper.selectReportForUpdate(10L)).thenReturn(pendingReport("POST"));

        assertThatThrownBy(() -> adminReportService.processReport(10L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("RESTRICTION_NOT_ALLOWED"));
    }

    @Test
    // AI작성: 게시글 신고 승인 시 삭제 선택값이 참이면 신고 대상 게시글을 함께 소프트 삭제한다.
    void confirmedPostReportDeletesTargetContentWhenRequested() {
        LocalDateTime restrictedUntil = LocalDateTime.of(2026, 11, 6, 10, 0);
        AdminReportProcessRequest request = new AdminReportProcessRequest();
        request.setStatus("CONFIRMED");
        request.setRestrictionScope(MemberRestrictionScope.CONTENT);
        request.setRestrictionType("7_DAYS");
        request.setDeleteTargetContent(true);
        request.setProcessReason("게시글 운영정책 위반");
        AdminMemberRestrictionTarget target = new AdminMemberRestrictionTarget();
        target.setMemberId(2L);
        target.setAccountStatus("ACTIVE");

        when(adminReportMapper.selectReportForUpdate(10L)).thenReturn(pendingReport("POST"));
        when(adminReportMapper.selectReportedMemberId(10L)).thenReturn(2L);
        when(adminMemberMapper.selectRestrictionTargetForUpdate(2L, MemberRestrictionScope.CONTENT))
                .thenReturn(target);
        when(memberRestrictionPolicy.calculateRestrictedUntil(eq(false), eq(null), eq("7_DAYS"), any()))
                .thenReturn(restrictedUntil);
        when(adminMemberMapper.insertRestrictionHistory(
                2L,
                RestrictionAction.RESTRICT,
                "7_DAYS",
                MemberRestrictionScope.CONTENT,
                "게시글 운영정책 위반",
                restrictedUntil
        )).thenReturn(1);
        when(adminReportMapper.updateReportProcess(
                10L,
                "CONFIRMED",
                "게시글 운영정책 위반",
                restrictedUntil,
                MemberRestrictionScope.CONTENT
        )).thenReturn(1);

        adminReportService.processReport(10L, request);

        verify(adminReportMapper).deleteReportedPost(10L);
    }

    private AdminReportListItemResponse pendingReport(String reportType) {
        AdminReportListItemResponse report = new AdminReportListItemResponse();
        report.setStatus("PENDING");
        report.setReportType(reportType);
        return report;
    }
}
