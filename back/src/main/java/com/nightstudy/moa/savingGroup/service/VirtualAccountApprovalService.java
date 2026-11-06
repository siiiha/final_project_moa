package com.nightstudy.moa.savingGroup.service;

// AI작성: 기존 승인 트랜잭션 내부에서 계좌 발급·저장과 실패 이력 보존을 담당한다.
public interface VirtualAccountApprovalService {
    void issueAndSave(Long approvalId, Long groupId);
    void requireRejectable(Long approvalId);
}
