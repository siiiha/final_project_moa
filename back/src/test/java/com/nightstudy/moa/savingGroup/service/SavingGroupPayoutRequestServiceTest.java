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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 내부 Mock으로 금융 호출 없이 계주·모임 상태·유형·페이지 경계를 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupPayoutRequestServiceTest {
    @Mock SavingGroupMapper mapper;
    @InjectMocks SavingGroupServiceImpl service;
    void target(String status, long ownerId) {
        var row = new SavingGroupJoinTargetRow();
        row.setSavingGroupId(10L); row.setGroupId(20L); row.setStatus(status); row.setOwnerMemberId(ownerId);
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(row);
    }
    void relation(String role, String status) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(row);
    }
    void authorized(String type) {
        target("02", 30L); relation("OWNER", "APPROVED");
        when(mapper.selectSavingGroupPayoutType(20L)).thenReturn(type);
    }
    void fails(String code, Long memberId, String status, int page) {
        assertThatThrownBy(() -> service.getSavingGroupPayoutRequests(10L, memberId, status, page))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
        verify(mapper, never()).selectSavingGroupPayoutRequests(anyLong(), anyString(), any(), anyInt());
    }
    @ParameterizedTest @CsvSource({"순번계,PENDING", "순번계,APPROVED", "순번계,REJECTED", "순번계,COMPLETED", "친목계,PENDING", "친목계,APPROVED", "친목계,REJECTED"})
    void ownerGetsSecondPageWithStatus(String type, String status) {
        authorized(type);
        var item = new SavingGroupPayoutRequestResponse(); item.setRequestId("42");
        when(mapper.selectSavingGroupPayoutRequestCount(20L, type, status)).thenReturn(11);
        when(mapper.selectSavingGroupPayoutRequests(20L, type, status, 10)).thenReturn(List.of(item));
        var result = service.getSavingGroupPayoutRequests(10L, 30L, status, 2);
        assertThat(result.getItems()).containsExactly(item);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
    }
    @Test void emptyAndHugePageReturnEmptyWithoutOffsetQuery() {
        authorized("순번계");
        assertThat(service.getSavingGroupPayoutRequests(10L, 30L, null, 1).getItems()).isEmpty();
        when(mapper.selectSavingGroupPayoutRequestCount(20L, "순번계", null)).thenReturn(11);
        assertThat(service.getSavingGroupPayoutRequests(10L, 30L, null, Integer.MAX_VALUE).getItems()).isEmpty();
        verify(mapper, never()).selectSavingGroupPayoutRequests(anyLong(), anyString(), any(), anyInt());
    }
    @ParameterizedTest @CsvSource({"MANAGER,APPROVED", "MEMBER,APPROVED", "OWNER,LEFT", "OWNER,PENDING", "OWNER,REJECTED", "OWNER,EXPELLED"})
    void forbiddenRelations(String role, String status) {
        target("01", 30L); relation(role, status); fails("SAVING_GROUP_OWNER_REQUIRED", 30L, null, 1);
    }
    @Test void nonMemberDenied() { target("01", 30L); fails("SAVING_GROUP_OWNER_REQUIRED", 30L, null, 1); }
    @Test void anotherGroupsOwnerDenied() { target("01", 40L); fails("SAVING_GROUP_OWNER_REQUIRED", 30L, null, 1); }
    @ParameterizedTest @ValueSource(strings = {"00", "03", "04", "05"})
    void inactiveGroupDenied(String status) { target(status, 30L); fails("SAVING_GROUP_ACTIVITY_RESTRICTED", 30L, null, 1); }
    @Test void missingAuthentication() { fails("UNAUTHORIZED", null, null, 1); verifyNoInteractions(mapper); }
    @Test void missingGroup() { fails("SAVING_GROUP_NOT_FOUND", 30L, null, 1); }
    @Test void invalidPageAndFilter() { fails("BAD_REQUEST", 30L, null, 0); fails("BAD_REQUEST", 30L, "UNKNOWN", 1); verifyNoInteractions(mapper); }
    @Test void socialHasNoCompletedStatus() { authorized("친목계"); fails("BAD_REQUEST", 30L, "COMPLETED", 1); }
    @Test void unknownTypeDenied() { authorized("미정"); fails("SAVING_GROUP_TYPE_INVALID", 30L, null, 1); }
}
