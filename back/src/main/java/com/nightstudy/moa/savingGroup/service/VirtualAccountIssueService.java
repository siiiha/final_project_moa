package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;

// AI작성: 발급 책임을 승인 로직에서 분리하며 설정 미완료를 사전에 검증한다.
public interface VirtualAccountIssueService {
    void requireConfigured();
    VirtualAccountIssueResult issue(VirtualAccountIssueRequest request);
    String accountHolderName();
}
