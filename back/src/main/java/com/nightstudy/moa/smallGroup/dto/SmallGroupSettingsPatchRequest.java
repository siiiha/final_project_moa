package com.nightstudy.moa.smallGroup.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI작성: PATCH 본문에서 실제로 전달된 소모임 설정 필드만 검증하고 수정 대상으로 구분한다.
@Getter
@NoArgsConstructor
public class SmallGroupSettingsPatchRequest {
    private String groupName;
    private String likeCategory;
    private String description;
    private Integer maxMembers;
    private LocalDate meetDate;
    private String location;

    private boolean groupNameProvided;
    private boolean likeCategoryProvided;
    private boolean descriptionProvided;
    private boolean maxMembersProvided;
    private boolean meetDateProvided;
    private boolean locationProvided;

    @JsonSetter("groupName")
    public void setGroupName(String groupName) {
        this.groupNameProvided = true;
        this.groupName = groupName;
    }

    @JsonSetter("likeCategory")
    public void setLikeCategory(String likeCategory) {
        this.likeCategoryProvided = true;
        this.likeCategory = likeCategory;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.descriptionProvided = true;
        this.description = description;
    }

    @JsonSetter("maxMembers")
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembersProvided = true;
        this.maxMembers = maxMembers;
    }

    @JsonSetter("meetDate")
    public void setMeetDate(LocalDate meetDate) {
        this.meetDateProvided = true;
        this.meetDate = meetDate;
    }

    @JsonSetter("location")
    public void setLocation(String location) {
        this.locationProvided = true;
        this.location = location;
    }

    @AssertTrue(message = "수정할 정보를 하나 이상 입력해 주세요.")
    public boolean isAnyFieldProvided() {
        return groupNameProvided || likeCategoryProvided || descriptionProvided || maxMembersProvided
                || meetDateProvided || locationProvided;
    }

    @AssertTrue(message = "소모임 이름은 null일 수 없습니다.")
    public boolean isGroupNameNotNull() {
        return !groupNameProvided || groupName != null;
    }

    @AssertTrue(message = "소모임 이름은 공백을 제외하고 2자 이상 20자 이하여야 합니다.")
    public boolean isGroupNameLengthValid() {
        return !groupNameProvided || groupName == null
                || (groupName.strip().length() >= 2 && groupName.strip().length() <= 20);
    }

    @AssertTrue(message = "관심사 카테고리는 null일 수 없습니다.")
    public boolean isLikeCategoryNotNull() {
        return !likeCategoryProvided || likeCategory != null;
    }

    @AssertTrue(message = "관심사 카테고리를 입력해 주세요.")
    public boolean isLikeCategoryNotBlank() {
        return !likeCategoryProvided || likeCategory == null || !likeCategory.strip().isEmpty();
    }

    @AssertTrue(message = "관심사 카테고리는 50자 이내로 입력해 주세요.")
    public boolean isLikeCategoryLengthValid() {
        return !likeCategoryProvided || likeCategory == null || likeCategory.strip().length() <= 50;
    }

    @AssertTrue(message = "소개글은 null일 수 없습니다.")
    public boolean isDescriptionNotNull() {
        return !descriptionProvided || description != null;
    }

    @AssertTrue(message = "소개글은 공백을 제외하고 40자 이하여야 합니다.")
    public boolean isDescriptionLengthValid() {
        return !descriptionProvided || description == null || description.strip().length() <= 40;
    }

    @AssertTrue(message = "정원은 null일 수 없습니다.")
    public boolean isMaxMembersNotNull() {
        return !maxMembersProvided || maxMembers != null;
    }

    @AssertTrue(message = "정원은 2명 이상 20명 이하여야 합니다.")
    public boolean isMaxMembersRangeValid() {
        return !maxMembersProvided || maxMembers == null || (maxMembers >= 2 && maxMembers <= 20);
    }

    @AssertTrue(message = "만남일은 null일 수 없습니다.")
    public boolean isMeetDateNotNull() {
        return !meetDateProvided || meetDate != null;
    }

    @AssertTrue(message = "만남일은 내일부터 선택할 수 있습니다.")
    public boolean isMeetDateFuture() {
        return !meetDateProvided || meetDate == null || meetDate.isAfter(LocalDate.now());
    }

    @AssertTrue(message = "장소는 null일 수 없습니다.")
    public boolean isLocationNotNull() {
        return !locationProvided || location != null;
    }

    @AssertTrue(message = "만남 장소를 입력해 주세요.")
    public boolean isLocationNotBlank() {
        return !locationProvided || location == null || !location.strip().isEmpty();
    }

    @AssertTrue(message = "만남 장소는 40자 이내로 입력해 주세요.")
    public boolean isLocationLengthValid() {
        return !locationProvided || location == null || location.strip().length() <= 40;
    }
}
