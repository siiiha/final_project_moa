package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.VirtualAccountIssueResult;

// AI작성: 계좌 저장과 완료 상태 변경은 호출자의 승인 트랜잭션에 참여한다.
public interface VirtualAccountStorageService {
    void requireConfigured();
    void save(Long approvalId, Long groupId, VirtualAccountIssueResult result, String holder);
}
