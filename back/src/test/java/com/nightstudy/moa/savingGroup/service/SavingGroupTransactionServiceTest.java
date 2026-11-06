package com.nightstudy.moa.savingGroup.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.*;
import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.math.BigDecimal;
// AI작성: 내부 Mock Mapper만 사용해 승인 관계와 금융 데이터 접근 순서를 검증한다.
class SavingGroupTransactionServiceTest {
    SavingGroupMapper mapper = mock(SavingGroupMapper.class);
    VirtualAccountStorageMapper accounts = mock(VirtualAccountStorageMapper.class);
    SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper, accounts, mock(AccountNumberCipher.class));
    SavingGroupTransactionSearchCondition condition = new SavingGroupTransactionSearchCondition();
    void member(String role, String status) {
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        var relation = new SavingGroupMembershipRelationRow();
        relation.setMemberRole(role); relation.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(2L, 1L)).thenReturn(relation);
    }
    void account() {
        var row = new EncryptedGroupAccountRow(); row.setAccountId(3L);
        when(accounts.selectAccount(2L)).thenReturn(row);
    }
    void fails(HttpStatus status, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(status));
    }
    @ParameterizedTest @ValueSource(strings={"OWNER","MANAGER","MEMBER"})
    void allApprovedRolesReadFilteredSecondPage(String role) {
        member(role,"APPROVED"); account(); condition.setPage(2); condition.setSort("OLDEST");
        condition.setTransactionType("WITHDRAWAL"); condition.setMinAmount(new BigDecimal("10000")); condition.setMaxAmount(new BigDecimal("40000"));
        when(mapper.selectSavingGroupTransactionCount(3L, condition)).thenReturn(11);
        var row = new SavingGroupTransactionResponse(); row.setAmount(new BigDecimal("10000"));
        when(mapper.selectSavingGroupTransactions(3L, condition, 10)).thenReturn(List.of(row));
        var result = service.getSavingGroupTransactions(2L,1L,condition);
        assertThat(result.getItems()).containsExactly(row); assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
        assertThat(result.getPageMeta().getSize()).isEqualTo(10);
        verify(mapper).selectSavingGroupTransactions(3L,condition,10);
    }
    @ParameterizedTest @ValueSource(strings={"PENDING","REJECTED","LEFT","EXPELLED"})
    void deniesNonCurrentMembersBeforeAccountRead(String status) {
        member("MEMBER",status); fails(HttpStatus.FORBIDDEN, () -> service.getSavingGroupTransactions(2L,1L,condition));
        verifyNoInteractions(accounts); verify(mapper,never()).selectSavingGroupTransactionCount(any(),any());
    }
    @Test void absentMembershipOrOtherGroupMemberIsForbidden() {
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        fails(HttpStatus.FORBIDDEN, () -> service.getSavingGroupTransactions(2L,1L,condition));
        verifyNoInteractions(accounts);
    }
    // AI작성: 승인 상태라도 정의되지 않은 역할이나 누락된 역할은 허용하지 않는다.
    @ParameterizedTest @org.junit.jupiter.params.provider.NullAndEmptySource
    @ValueSource(strings={"ADMIN", "ACCOUNTANT"})
    void deniesUnknownRoles(String role) {
        member(role,"APPROVED");
        fails(HttpStatus.FORBIDDEN, () -> service.getSavingGroupTransactions(2L,1L,condition));
        verifyNoInteractions(accounts);
    }
    @Test void unauthenticatedAndMissingTargets() {
        fails(HttpStatus.UNAUTHORIZED, () -> service.getSavingGroupTransactions(2L,null,condition));
        verifyNoInteractions(mapper,accounts);
        fails(HttpStatus.NOT_FOUND, () -> service.getSavingGroupTransactions(2L,1L,condition));
        member("MEMBER","APPROVED");
        fails(HttpStatus.NOT_FOUND, () -> service.getSavingGroupTransactions(2L,1L,condition));
    }
    @Test void emptyAndOutOfRangePagesAreSuccessfulEmptyLists() {
        member("MEMBER","APPROVED"); account();
        assertThat(service.getSavingGroupTransactions(2L,1L,condition).getItems()).isEmpty();
        condition.setPage(3); when(mapper.selectSavingGroupTransactionCount(3L,condition)).thenReturn(11);
        assertThat(service.getSavingGroupTransactions(2L,1L,condition).getItems()).isEmpty();
        verify(mapper,never()).selectSavingGroupTransactions(any(),any(),anyInt());
    }
    @Test void invalidConditionsNeverReadFinancialData() {
        condition.setPage(0); fails(HttpStatus.BAD_REQUEST, () -> service.getSavingGroupTransactions(2L,1L,condition));
        condition.setPage(1); condition.setSort("INVALID"); fails(HttpStatus.BAD_REQUEST, () -> service.getSavingGroupTransactions(2L,1L,condition));
        condition.setSort("LATEST"); condition.setMinAmount(new BigDecimal("40000")); condition.setMaxAmount(new BigDecimal("10000"));
        fails(HttpStatus.BAD_REQUEST, () -> service.getSavingGroupTransactions(2L,1L,condition)); verifyNoInteractions(mapper,accounts);
    }
}
