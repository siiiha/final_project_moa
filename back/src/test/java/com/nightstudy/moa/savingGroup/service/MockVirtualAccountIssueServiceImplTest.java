package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.VirtualAccountIssueRequest;
import org.junit.jupiter.api.Test;

// AI작성: 실제 네트워크 없이 테스트 전용 계좌 결과만 생성한다.
class MockVirtualAccountIssueServiceImplTest {
    @Test void issuesSyntheticResult() {
        var result = new MockVirtualAccountIssueServiceImpl().issue(new VirtualAccountIssueRequest(1L, 2L, "moa_test"));
        assertThat(result.getAccountNumber()).startsWith("X");
        assertThat(result.getDueDate()).isAfter(java.time.OffsetDateTime.now());
    }
}
