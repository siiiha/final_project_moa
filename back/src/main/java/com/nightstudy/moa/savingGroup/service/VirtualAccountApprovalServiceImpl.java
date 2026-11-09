package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;

// AI작성: 외부 요청 결과와 DB 커밋 결과를 구분하고 롤백 시 이력을 별도 보존한다.
@Service
public class VirtualAccountApprovalServiceImpl implements VirtualAccountApprovalService {
    private final VirtualAccountIssueService issuer;
    private final VirtualAccountStorageService storage;
    private final VirtualAccountIssueStateService state;
    public VirtualAccountApprovalServiceImpl(VirtualAccountIssueService issuer,
            VirtualAccountStorageService storage, VirtualAccountIssueStateService state) {
        this.issuer = issuer; this.storage = storage; this.state = state;
    }
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void issueAndSave(Long approvalId, Long groupId) {
        issuer.requireConfigured(); storage.requireConfigured();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("승인 트랜잭션이 필요합니다.");
        var request = state.begin(approvalId, groupId);
        // AI작성: 정상 발급 뒤 기존 승인 또는 커밋이 실패해도 UNKNOWN으로 보존한다.
        String[] failure = {"UNKNOWN", "ACCOUNT_ISSUE_UNKNOWN"};
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { state.recordFailure(approvalId, failure[0], failure[1]); }
                    catch (RuntimeException ignored) {
                        // AI작성: 기록 실패 시 REQUESTED가 남아 재발급을 계속 차단한다. 예외 원문은 로그에 남기지 않는다.
                    }
                }
            }
        });
        try {
            var result = issuer.issue(request);
            storage.save(approvalId, groupId, result, issuer.accountHolderName());
        } catch (ApiException e) {
            if ("ACCOUNT_ISSUE_REJECTED".equals(e.getCode())) {
                failure[0] = "FAILED"; failure[1] = "ACCOUNT_ISSUE_REJECTED";
            }
            throw e;
        }
    }
    @Override public void requireRejectable(Long approvalId) { state.requireRejectable(approvalId); }
}
