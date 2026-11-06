package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: accountNumber에는 원문이 아닌 버전·키 식별자가 포함된 암호문만 저장한다.
@Getter
@Setter
public class EncryptedGroupAccountRow {
    private Long accountId;
    private Long groupId;
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
}
