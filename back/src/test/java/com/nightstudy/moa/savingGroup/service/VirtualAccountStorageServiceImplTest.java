package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;

// AI작성: DB 경계에 계좌 원문이 전달되지 않으며 저장 실패가 성공으로 처리되지 않는지 검증한다.
class VirtualAccountStorageServiceImplTest {
    @Test void encryptsBeforeInsert() {
        var mapper = mock(VirtualAccountStorageMapper.class);
        var service = new VirtualAccountStorageServiceImpl(mapper, AccountNumberCipherTest.cipher());
        when(mapper.insertAccount(any())).thenAnswer(call -> {
            EncryptedGroupAccountRow row = call.getArgument(0);
            assertThat(row.getAccountNumber()).startsWith("v1:test:").doesNotContain("X000000000000");
            row.setAccountId(3L); return 1;
        });
        when(mapper.completeIssue(1L, 3L)).thenReturn(1);
        service.save(1L, 2L, new VirtualAccountIssueResult("00", "X000000000000", OffsetDateTime.now().plusDays(7)), "모아테스트");
        verify(mapper).completeIssue(1L, 3L);
    }
    @Test void rejectsInvalidOrFailedStorage() {
        var mapper = mock(VirtualAccountStorageMapper.class);
        var service = new VirtualAccountStorageServiceImpl(mapper, AccountNumberCipherTest.cipher());
        assertThatThrownBy(() -> service.save(1L, 2L, null, "모아테스트")).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(mapper);
        assertThatThrownBy(() -> service.save(1L, 2L,
                new VirtualAccountIssueResult("00", "X000000000000", OffsetDateTime.now().plusDays(7)), "모아테스트"))
                .isInstanceOf(RuntimeException.class);
        verify(mapper, never()).completeIssue(any(), any());
    }
}
