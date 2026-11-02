package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListItemResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionReleaseRequest;
import com.nightstudy.moa.admin.dto.MemberRestrictionAction;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminMemberFeatureRestrictionResponse;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 관리자 회원 Service의 페이징과 상세 조회 예외를 DB 없이 검증한다.
class AdminMemberServiceImplTest {
    @Mock
    private AdminMemberMapper adminMemberMapper;

    @Mock
    private MemberRestrictionPolicy memberRestrictionPolicy;

    @InjectMocks
    private AdminMemberServiceImpl adminMemberService;

    @Test
    void getMemberListReturnsItemsAndPageMeta() {
        AdminMemberSearchCondition condition = new AdminMemberSearchCondition();
        condition.setPage(2);
        condition.setSize(10);
        AdminMemberListItemResponse member = new AdminMemberListItemResponse();
        member.setMemberId(11L);
        when(adminMemberMapper.selectMemberCount(condition)).thenReturn(11);
        when(adminMemberMapper.selectMemberList(condition)).thenReturn(List.of(member));

        AdminMemberListResult result = adminMemberService.getMemberList(condition);

        assertThat(result.getMembers()).containsExactly(member);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
        assertThat(condition.getOffset()).isEqualTo(10);
        assertThat(condition.getLimit()).isEqualTo(10);
    }

    @Test
    void getMemberListReturnsEmptyWithoutListQuery() {
        AdminMemberSearchCondition condition = new AdminMemberSearchCondition();
        when(adminMemberMapper.selectMemberCount(condition)).thenReturn(0);

        AdminMemberListResult result = adminMemberService.getMemberList(condition);

        assertThat(result.getMembers()).isEmpty();
        verify(adminMemberMapper, never()).selectMemberList(condition);
    }

    @Test
    void getMemberDetailReturnsDetail() {
        AdminMemberDetailResponse detail = new AdminMemberDetailResponse();
        detail.setMemberId(1L);
        when(adminMemberMapper.selectMemberDetail(1L)).thenReturn(detail);
        AdminMemberFeatureRestrictionResponse restriction = new AdminMemberFeatureRestrictionResponse();
        restriction.setRestrictionScope(MemberRestrictionScope.CONTENT);
        when(adminMemberMapper.selectMemberFeatureRestrictions(1L)).thenReturn(List.of(restriction));

        AdminMemberDetailResponse result = adminMemberService.getMemberDetail(1L);

        assertThat(result).isSameAs(detail);
        assertThat(result.getRestrictions()).containsExactly(restriction);
    }

    @Test
    void getMemberDetailThrowsWhenNotFound() {
        when(adminMemberMapper.selectMemberDetail(999L)).thenReturn(null);

        assertThatThrownBy(() -> adminMemberService.getMemberDetail(999L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("MEMBER_NOT_FOUND");
                });
    }

    @Test
    void restrictMemberUpsertsSelectedScopeAndSavesHistory() {
        AdminMemberRestrictionTarget target = new AdminMemberRestrictionTarget();
        target.setMemberId(2L);
        target.setAccountStatus("ACTIVE");
        LocalDateTime restrictedUntil = LocalDateTime.of(2026, 11, 5, 10, 0);
        AdminMemberRestrictionRequest request = new AdminMemberRestrictionRequest();
        request.setRestrictionScope(MemberRestrictionScope.CONTENT);
        request.setRestrictionType("7_DAYS");
        request.setReason("운영정책 위반");

        when(adminMemberMapper.selectRestrictionTargetForUpdate(2L, MemberRestrictionScope.CONTENT))
                .thenReturn(target);
        when(memberRestrictionPolicy.calculateRestrictedUntil(eq(false), eq(null), eq("7_DAYS"), any()))
                .thenReturn(restrictedUntil);
        when(adminMemberMapper.insertRestrictionHistory(
                2L,
                MemberRestrictionAction.RESTRICT,
                "7_DAYS",
                MemberRestrictionScope.CONTENT,
                "운영정책 위반",
                restrictedUntil
        )).thenReturn(1);

        adminMemberService.restrictMember(1L, 2L, request);

        verify(adminMemberMapper).upsertMemberRestriction(
                2L,
                MemberRestrictionScope.CONTENT,
                restrictedUntil
        );
    }

    @Test
    void restrictMemberRejectsAdministratorSelfRestriction() {
        AdminMemberRestrictionRequest request = new AdminMemberRestrictionRequest();
        request.setRestrictionScope(MemberRestrictionScope.CONTENT);

        assertThatThrownBy(() -> adminMemberService.restrictMember(1L, 1L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("ADMIN_SELF_RESTRICTION_NOT_ALLOWED"));
        verify(adminMemberMapper, never()).selectRestrictionTargetForUpdate(any(), any());
    }

    @Test
    void releaseMemberRestrictionDeletesOnlySelectedScopeAndSavesHistory() {
        AdminMemberRestrictionTarget target = new AdminMemberRestrictionTarget();
        target.setMemberId(2L);
        target.setAccountStatus("ACTIVE");
        target.setRestrictionScope(MemberRestrictionScope.CHAT);
        AdminMemberRestrictionReleaseRequest request = new AdminMemberRestrictionReleaseRequest();
        request.setRestrictionScope(MemberRestrictionScope.CHAT);
        request.setReason("관리자 재검토");

        when(adminMemberMapper.selectRestrictionTargetForUpdate(2L, MemberRestrictionScope.CHAT))
                .thenReturn(target);
        when(adminMemberMapper.releaseMemberRestriction(2L, MemberRestrictionScope.CHAT)).thenReturn(1);
        when(adminMemberMapper.insertRestrictionHistory(
                2L,
                MemberRestrictionAction.RELEASE,
                null,
                MemberRestrictionScope.CHAT,
                "관리자 재검토",
                null
        )).thenReturn(1);

        adminMemberService.releaseMemberRestriction(1L, 2L, request);

        verify(adminMemberMapper).releaseMemberRestriction(2L, MemberRestrictionScope.CHAT);
    }
}
