package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.*;
import com.nightstudy.moa.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;

// AI작성: 권한 검사 이전에는 암호문을 읽지 않고 승인 모임원에게만 마스킹 결과를 반환한다.
class SavingGroupAccountServiceTest {
    SavingGroupMapper mapper = mock(SavingGroupMapper.class);
    VirtualAccountStorageMapper accounts = mock(VirtualAccountStorageMapper.class);
    AccountNumberCipher cipher = AccountNumberCipherTest.cipher();
    SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper, accounts, cipher);
    void member(String role, String status) {
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        var relation = new SavingGroupMembershipRelationRow(); relation.setMemberRole(role); relation.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(2L, 1L)).thenReturn(relation);
    }
    @ParameterizedTest @ValueSource(strings = {"OWNER", "MANAGER", "MEMBER"})
    void returnsMaskedAccountForApprovedRoles(String role) {
        member(role, "APPROVED");
        var account = new EncryptedGroupAccountRow(); account.setBankCode("00");
        account.setAccountNumber(cipher.encrypt(2L, "X000000000000"));
        when(accounts.selectAccount(2L)).thenReturn(account); when(mapper.selectSavingGroupBalance(2L)).thenReturn(BigDecimal.ZERO);
        var result = service.getSavingGroupAccount(2L, 1L);
        assertThat(result.issued()).isTrue(); assertThat(result.maskedAccountNumber()).isEqualTo("*********0000");
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void deniesUnapprovedMembership(String status) {
        member("MEMBER", status);
        assertThatThrownBy(() -> service.getSavingGroupAccount(2L, 1L)).isInstanceOf(ApiException.class);
        verifyNoInteractions(accounts);
    }
    @Test void distinguishesNotIssuedAndAuthentication() {
        assertThatThrownBy(() -> service.getSavingGroupAccount(2L, null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(mapper, accounts);
        member("MEMBER", "APPROVED");
        assertThat(service.getSavingGroupAccount(2L, 1L).issued()).isFalse();
    }
    @Test void missingGroupOrOtherGroupMemberCannotReadAccount() {
        assertThatThrownBy(() -> service.getSavingGroupAccount(2L, 1L)).isInstanceOf(ApiException.class);
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        assertThatThrownBy(() -> service.getSavingGroupAccount(2L, 1L)).isInstanceOf(ApiException.class);
        verifyNoInteractions(accounts);
    }
}
