package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRequestSearchCondition;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

// AI작성: 기존 Validation을 이용해 기본값과 잘못된 페이지·상태 입력을 검증한다.
class SavingGroupPayoutRequestSearchConditionTest {
    @Test void validatesPageAndExistingStatuses() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var condition = new SavingGroupPayoutRequestSearchCondition();
            assertThat(validator.validate(condition)).isEmpty();
            assertThat(condition.getPage()).isEqualTo(1);
            for (String status : new String[]{"PENDING", "APPROVED", "REJECTED", "COMPLETED"}) {
                condition.setStatus(status);
                assertThat(validator.validate(condition)).isEmpty();
            }
            condition.setStatus("UNKNOWN"); condition.setPage(0);
            assertThat(validator.validate(condition)).hasSize(2);
            condition.setStatus(null); condition.setPage(null);
            assertThat(validator.validate(condition)).hasSize(1);
        }
    }
}
