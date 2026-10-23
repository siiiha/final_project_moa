package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListItemResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.common.exception.ApiException;
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

        assertThat(adminMemberService.getMemberDetail(1L)).isSameAs(detail);
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
}
