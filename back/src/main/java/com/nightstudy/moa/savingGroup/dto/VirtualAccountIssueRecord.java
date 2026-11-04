package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 별도 커밋되는 발급 상태에는 계좌번호와 외부 오류 원문을 저장하지 않는다.
@Getter
@Setter
public class VirtualAccountIssueRecord {
    private Long issueId;
    private Long approvalId;
    private Long groupId;
    private String orderId;
    private String status;
    private Long accountId;
    private String errorCode;
}
