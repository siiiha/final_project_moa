package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.VirtualAccountIssueRequest;

// AI작성: 승인 롤백에도 유지되어야 하는 이력 작업을 별도 트랜잭션으로 분리한다.
public interface VirtualAccountIssueStateService {
    VirtualAccountIssueRequest begin(Long approvalId, Long groupId);
    void recordFailure(Long approvalId, String status, String code);
    void requireRejectable(Long approvalId);
}
