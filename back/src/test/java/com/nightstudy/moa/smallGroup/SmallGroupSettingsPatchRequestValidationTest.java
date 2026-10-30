package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// AI작성: 소모임 수정 PATCH의 전달 필드 구분과 확정된 입력 범위를 Controller 이전에 검증한다.
class SmallGroupSettingsPatchRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidPartialUpdate() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setGroupName("  동네 걷기  ");
        request.setLikeCategory("운동");
        request.setDescription("함께 가볍게 걸어요.");
        request.setMaxMembers(10);
        request.setMeetDate(LocalDate.now().plusDays(1));
        request.setLocation("한강 공원");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsRequestWithoutAnyUpdateField() {
        assertThat(messages(new SmallGroupSettingsPatchRequest()))
                .contains("수정할 정보를 하나 이상 입력해 주세요.");
    }

    @Test
    void rejectsInvalidNameAndDescriptionLengths() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setGroupName("가");
        request.setDescription("가".repeat(41));

        assertThat(messages(request))
                .contains("소모임 이름은 공백을 제외하고 2자 이상 20자 이하여야 합니다.",
                        "소개글은 공백을 제외하고 40자 이하여야 합니다.");
    }

    @Test
    void rejectsCapacityOutsideTwoToTwenty() {
        SmallGroupSettingsPatchRequest belowMinimum = new SmallGroupSettingsPatchRequest();
        belowMinimum.setMaxMembers(1);
        SmallGroupSettingsPatchRequest aboveMaximum = new SmallGroupSettingsPatchRequest();
        aboveMaximum.setMaxMembers(21);

        assertThat(messages(belowMinimum)).contains("정원은 2명 이상 20명 이하여야 합니다.");
        assertThat(messages(aboveMaximum)).contains("정원은 2명 이상 20명 이하여야 합니다.");
    }

    @Test
    void rejectsTodayOrPastMeetingDate() {
        SmallGroupSettingsPatchRequest today = new SmallGroupSettingsPatchRequest();
        today.setMeetDate(LocalDate.now());
        SmallGroupSettingsPatchRequest past = new SmallGroupSettingsPatchRequest();
        past.setMeetDate(LocalDate.now().minusDays(1));

        assertThat(messages(today)).contains("만남일은 내일부터 선택할 수 있습니다.");
        assertThat(messages(past)).contains("만남일은 내일부터 선택할 수 있습니다.");
    }

    @Test
    void rejectsBlankCategoryAndLocation() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setLikeCategory("  ");
        request.setLocation("  ");

        assertThat(messages(request))
                .contains("관심사 카테고리를 입력해 주세요.", "만남 장소를 입력해 주세요.");
    }

    private Set<String> messages(SmallGroupSettingsPatchRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
    }
}
