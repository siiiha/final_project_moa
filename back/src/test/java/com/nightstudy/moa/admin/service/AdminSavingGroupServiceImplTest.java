package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import com.nightstudy.moa.admin.mapper.AdminSavingGroupMapper;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 관리자 계모임 Service의 페이징과 상세 조회 예외를 DB 없이 검증한다.
class AdminSavingGroupServiceImplTest {
    @Mock
    private AdminSavingGroupMapper adminSavingGroupMapper;

    @Mock
    private SavingGroupMapper savingGroupMapper;

    @InjectMocks
    private AdminSavingGroupServiceImpl adminSavingGroupService;

    @Test
    void getSavingGroupListReturnsItemsAndPageMeta() {
        AdminSavingGroupSearchCondition condition = new AdminSavingGroupSearchCondition();
        condition.setPage(2);
        condition.setSize(10);
        AdminSavingGroupListItemResponse item = new AdminSavingGroupListItemResponse();
        item.setSavingGroupId(11L);
        item.setGroupId(101L);
        when(adminSavingGroupMapper.selectSavingGroupCount(condition)).thenReturn(11);
        when(adminSavingGroupMapper.selectSavingGroupList(condition)).thenReturn(List.of(item));
        when(savingGroupMapper.selectApprovedMemberCounts(List.of(101L))).thenReturn(List.of());

        AdminSavingGroupListResult result = adminSavingGroupService.getSavingGroupList(condition);

        assertThat(result.getSavingGroups()).containsExactly(item);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
    }

    @Test
    void getSavingGroupListReturnsEmptyWithoutListQuery() {
        AdminSavingGroupSearchCondition condition = new AdminSavingGroupSearchCondition();
        when(adminSavingGroupMapper.selectSavingGroupCount(condition)).thenReturn(0);

        AdminSavingGroupListResult result = adminSavingGroupService.getSavingGroupList(condition);

        assertThat(result.getSavingGroups()).isEmpty();
        verify(adminSavingGroupMapper, never()).selectSavingGroupList(condition);
    }

    @Test
    void getSavingGroupDetailReturnsDetail() {
        AdminSavingGroupDetailResponse detail = new AdminSavingGroupDetailResponse();
        detail.setSavingGroupId(1L);
        when(adminSavingGroupMapper.selectSavingGroupDetail(1L)).thenReturn(detail);

        assertThat(adminSavingGroupService.getSavingGroupDetail(1L)).isSameAs(detail);
    }

    @Test
    void getSavingGroupDetailThrowsWhenNotFound() {
        when(adminSavingGroupMapper.selectSavingGroupDetail(999L)).thenReturn(null);

        assertThatThrownBy(() -> adminSavingGroupService.getSavingGroupDetail(999L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_NOT_FOUND");
                });
    }
}
