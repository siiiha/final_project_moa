package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 서로 다른 savingGroupId/groupId로 권한·상태·최대 두 페이지와 빈 결과를 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupMemberListServiceTest {
    @Mock SavingGroupMapper mapper;
    @InjectMocks SavingGroupServiceImpl service;

    void target(String status, long ownerId) {
        var target = new SavingGroupJoinTargetRow();
        target.setGroupId(20L); target.setSavingGroupId(10L); target.setStatus(status); target.setOwnerMemberId(ownerId);
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(target);
    }
    void owner(String role, String status) {
        var owner = new SavingGroupMembershipRelationRow();
        owner.setMemberRole(role); owner.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(owner);
    }
    void error(String code, Long groupId, Long memberId, int page) {
        assertThatThrownBy(() -> service.getSavingGroupMembers(groupId, memberId, page))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
        verify(mapper, never()).selectSavingGroupMembers(anyLong(), any(), anyInt());
    }
    @ParameterizedTest @ValueSource(strings = {"01", "02"})
    void ownerReadsSecondPageCappedAtTwenty(String status) {
        target(status, 30L); owner("OWNER", "APPROVED");
        when(mapper.selectSavingGroupMemberCount(20L, null)).thenReturn(25);
        var row = new SavingGroupMemberResponse(); row.setMemberId("50");
        when(mapper.selectSavingGroupMembers(20L, null, 10)).thenReturn(List.of(row));
        var result = service.getSavingGroupMembers(10L, 30L, 2);
        assertThat(result.getItems()).containsExactly(row);
        assertThat(result.getPageMeta().getTotal()).isEqualTo(20);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
        assertThat(result.getPageMeta().getSize()).isEqualTo(10);
    }
    @ParameterizedTest @ValueSource(ints = {1, 3, Integer.MAX_VALUE})
    void emptyOrBeyondRangeDoesNotReadRows(int page) {
        target("01", 30L); owner("OWNER", "APPROVED");
        when(mapper.selectSavingGroupMemberCount(20L, null)).thenReturn(page == 1 ? 0 : 20);
        assertThat(service.getSavingGroupMembers(10L, 30L, page).getItems()).isEmpty();
        verify(mapper, never()).selectSavingGroupMembers(anyLong(), any(), anyInt());
    }
    @ParameterizedTest @ValueSource(ints = {1, 10, 11, 20})
    void pageMetaUsesTenRows(int count) {
        target("02", 30L); owner("OWNER", "APPROVED");
        when(mapper.selectSavingGroupMemberCount(20L, null)).thenReturn(count);
        when(mapper.selectSavingGroupMembers(20L, null, 0)).thenReturn(List.of());
        assertThat(service.getSavingGroupMembers(10L, 30L, 1).getPageMeta().getTotalPages())
                .isEqualTo((count + 9) / 10);
    }
    @Test void requiresLogin() { error("UNAUTHORIZED", 10L, null, 1); verifyNoInteractions(mapper); }
    @Test void invalidPage() { error("BAD_REQUEST", 10L, 30L, 0); }
    @Test void invalidGroup() { error("BAD_REQUEST", 0L, 30L, 1); }
    @Test void missingGroup() { error("SAVING_GROUP_NOT_FOUND", 10L, 30L, 1); }
    @Test void otherGroupOwnerCannotRead() { target("01", 99L); error("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L, 1); }
    @ParameterizedTest @ValueSource(strings = {"MANAGER", "MEMBER"})
    void nonOwnerCannotRead(String role) { target("01", 30L); owner(role, "APPROVED"); error("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L, 1); }
    @Test void nonMemberCannotRead() { target("01", 30L); error("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L, 1); }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "LEFT", "EXPELLED", "REJECTED"})
    void unapprovedOwnerCannotRead(String status) { target("01", 30L); owner("OWNER", status); error("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L, 1); }
    @ParameterizedTest @ValueSource(strings = {"00", "03", "04", "05"})
    void inactiveGroupCannotRead(String status) { target(status, 30L); error("SAVING_GROUP_ACTIVITY_RESTRICTED", 10L, 30L, 1); }
}
