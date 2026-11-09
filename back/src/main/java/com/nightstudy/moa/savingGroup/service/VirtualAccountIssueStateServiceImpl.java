package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

// AI작성: 이력 테이블에는 잠긴 신청·모임으로 향하는 외래키를 두지 않는 승인된 설계를 따른다.
@Service
public class VirtualAccountIssueStateServiceImpl implements VirtualAccountIssueStateService {
    private final VirtualAccountStorageMapper mapper;
    public VirtualAccountIssueStateServiceImpl(VirtualAccountStorageMapper mapper) { this.mapper = mapper; }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VirtualAccountIssueRequest begin(Long approvalId, Long groupId) {
        if (mapper.selectIssue(approvalId) != null || mapper.selectAccount(groupId) != null) throw blocked();
        var issue = new VirtualAccountIssueRecord();
        issue.setApprovalId(approvalId); issue.setGroupId(groupId);
        issue.setOrderId("moa_" + UUID.randomUUID());
        try {
            if (mapper.insertIssue(issue) != 1) throw blocked();
        } catch (DuplicateKeyException e) { throw blocked(); }
        return new VirtualAccountIssueRequest(approvalId, groupId, issue.getOrderId());
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long approvalId, String status, String code) {
        // AI작성: 임의 외부 오류 문자열이 DB에 들어가지 않도록 내부 상태·코드만 허용한다.
        if (!("UNKNOWN".equals(status) || "FAILED".equals(status))
                || !("ACCOUNT_ISSUE_REJECTED".equals(code) || "ACCOUNT_ISSUE_UNKNOWN".equals(code))) throw blocked();
        if (mapper.failIssue(approvalId, status, code) != 1) throw blocked();
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void requireRejectable(Long approvalId) {
        var issue = mapper.selectIssue(approvalId);
        if (issue != null && !"FAILED".equals(issue.getStatus())) throw blocked();
    }
    private ApiException blocked() {
        return new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ISSUE_REVIEW_REQUIRED", "계좌 발급 이력을 먼저 확인해 주세요. 자동 재발급은 지원하지 않습니다.");
    }
}
