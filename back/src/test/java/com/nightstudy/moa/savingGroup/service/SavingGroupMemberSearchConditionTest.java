package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMemberSearchCondition;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 기존 검색과 같은 공백 정규화·20자 제한·양수 페이지 Validation을 확인한다.
class SavingGroupMemberSearchConditionTest {
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t\n", "　"})
    void blankMeansNoSearch(String keyword) {
        var condition = new SavingGroupMemberSearchCondition(); condition.setKeyword(keyword);
        assertThat(condition.getKeyword()).isNull(); assertThat(condition.getPage()).isEqualTo(1);
    }
    @Test void normalizesBeforeLengthValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var condition = new SavingGroupMemberSearchCondition(); condition.setKeyword("  " + "가".repeat(20) + "  ");
            assertThat(condition.getKeyword()).isEqualTo("가".repeat(20)); assertThat(validator.validate(condition)).isEmpty();
            condition.setKeyword("가".repeat(21)); assertThat(validator.validate(condition)).isNotEmpty();
            condition.setKeyword(null); condition.setPage(0); assertThat(validator.validate(condition)).isNotEmpty();
            condition.setPage(null); assertThat(validator.validate(condition)).isNotEmpty();
        }
    }
}
