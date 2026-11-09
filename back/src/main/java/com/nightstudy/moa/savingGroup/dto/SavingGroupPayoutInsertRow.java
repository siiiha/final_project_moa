package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

// AI작성: 인증 사용자·확정 금액·서버 생성 증빙 경로만 저장하고 생성 키를 돌려받는다.
@Getter
@Setter
public class SavingGroupPayoutInsertRow {
    private Long id;
    private Long groupId;
    private Long roundId;
    private Long requesterId;
    private BigDecimal amount;
    private String title;
    private String receiptImgUrl;
    private String receiptSha256;
}
