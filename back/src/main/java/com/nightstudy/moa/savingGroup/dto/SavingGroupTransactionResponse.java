package com.nightstudy.moa.savingGroup.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
// AI작성: 계좌·거래 식별자와 적요를 공개 DTO에서 제외한다.
@Getter @Setter
public class SavingGroupTransactionResponse {
    private LocalDateTime transactedAt;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    // AI작성: 실제 은행 예금주가 아닌 현재 회원 정보의 마스킹된 테스트 입금 요청자다.
    private String depositRequesterName;
}
