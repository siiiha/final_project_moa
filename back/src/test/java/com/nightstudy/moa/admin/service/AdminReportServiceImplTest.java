package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminReportProcessRequest;
import com.nightstudy.moa.admin.dto.MemberRestrictionAction;
import com.nightstudy.moa.admin.dto.MemberRestrictionScope;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.admin.mapper.AdminReportMapper;
import com.nightstudy.moa.common.exception.ApiException;
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

        when(adminReportMapper.selectReportStatusForUpdate(10L)).thenReturn("PENDING");
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
                MemberRestrictionAction.RESTRICT,
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
        when(adminReportMapper.selectReportStatusForUpdate(10L)).thenReturn("PENDING");

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
        when(adminReportMapper.selectReportStatusForUpdate(10L)).thenReturn("PENDING");

        assertThatThrownBy(() -> adminReportService.processReport(10L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("RESTRICTION_NOT_ALLOWED"));
    }
}
