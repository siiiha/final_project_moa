package com.nightstudy.moa.savingGroup.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
// AI작성: 거래 조건을 검증하고 페이지 크기는 서버에서 10건으로 고정한다.
@Getter @Setter
public class SavingGroupTransactionSearchCondition {
    @NotNull @Min(1) @Max(214748364) private Integer page = 1;
    @NotNull @Pattern(regexp = "LATEST|OLDEST") private String sort = "LATEST";
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL") private String transactionType;
    @DecimalMin("0") @Digits(integer = 13, fraction = 2) private BigDecimal minAmount;
    @DecimalMin("0") @Digits(integer = 13, fraction = 2) private BigDecimal maxAmount;
    @AssertTrue(message = "최소 금액은 최대 금액보다 클 수 없습니다.")
    public boolean isAmountRangeValid() {
        return minAmount == null || maxAmount == null || minAmount.compareTo(maxAmount) <= 0;
    }
}
