package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.nightstudy.moa.savingGroup.dto.VirtualAccountIssueRecord;
import com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper;
import org.junit.jupiter.api.Test;

// AI작성: 발급 상태가 남아 있으면 실패·처리중 여부와 무관하게 자동 재발급하지 않는다.
class VirtualAccountIssueStateServiceTest {
    @Test void createsRandomOrderAndBlocksEveryExistingIssue() {
        var mapper = mock(VirtualAccountStorageMapper.class);
        var service = new VirtualAccountIssueStateServiceImpl(mapper);
        when(mapper.insertIssue(any())).thenReturn(1);
        assertThat(service.begin(1L, 2L).orderId()).matches("moa_[a-f0-9-]{36}");
        for (String status : new String[]{"REQUESTED", "COMPLETED", "FAILED", "UNKNOWN"}) {
            var issue = new VirtualAccountIssueRecord(); issue.setStatus(status);
            when(mapper.selectIssue(1L)).thenReturn(issue);
            assertThatThrownBy(() -> service.begin(1L, 2L)).isInstanceOf(RuntimeException.class);
        }
        verify(mapper, times(1)).insertIssue(any());
    }
    @Test void rejectsRejectionWhenIssueMayExist() {
        var mapper = mock(VirtualAccountStorageMapper.class);
        var service = new VirtualAccountIssueStateServiceImpl(mapper);
        for (String status : new String[]{"REQUESTED", "COMPLETED", "UNKNOWN"}) {
            var issue = new VirtualAccountIssueRecord(); issue.setStatus(status);
            when(mapper.selectIssue(1L)).thenReturn(issue);
            assertThatThrownBy(() -> service.requireRejectable(1L)).isInstanceOf(RuntimeException.class);
        }
    }
}
