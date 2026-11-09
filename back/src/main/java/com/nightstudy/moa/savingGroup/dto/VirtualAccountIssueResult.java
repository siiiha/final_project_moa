package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.OffsetDateTime;

// AI작성: 원문은 내부 발급·암호화 경계에서만 사용하고 자동 toString을 만들지 않는다.
@Getter
@AllArgsConstructor
public class VirtualAccountIssueResult {
    private final String bankCode;
    private final String accountNumber;
    private final OffsetDateTime dueDate;
}
