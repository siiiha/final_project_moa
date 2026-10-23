package com.nightstudy.moa.savingGroup.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SavingGroupSettingsPatchRequest {
    private String groupName;
    private String description;
    private Integer maxMembers;
    private Integer duesAmount;
    private Integer duesDay;

    private boolean groupNameProvided;
    private boolean descriptionProvided;
    private boolean maxMembersProvided;
    private boolean duesAmountProvided;
    private boolean duesDayProvided;

    @JsonSetter("groupName")
    public void setGroupName(String groupName) {
        this.groupNameProvided = true;
        this.groupName = groupName;
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

    @JsonSetter("duesAmount")
    public void setDuesAmount(Integer duesAmount) {
        this.duesAmountProvided = true;
        this.duesAmount = duesAmount;
    }

    @JsonSetter("duesDay")
    public void setDuesDay(Integer duesDay) {
        this.duesDayProvided = true;
        this.duesDay = duesDay;
    }

    @AssertTrue(message = "수정할 정보를 하나 이상 입력해 주세요.")
    public boolean isAnyFieldProvided() {
        return groupNameProvided || descriptionProvided || maxMembersProvided || duesAmountProvided || duesDayProvided;
    }

    @AssertTrue(message = "계모임 이름은 null일 수 없습니다.")
    public boolean isGroupNameNotNull() {
        return !groupNameProvided || groupName != null;
    }

    @AssertTrue(message = "계모임 이름은 공백을 제외하고 2자 이상 20자 이하여야 합니다.")
    public boolean isGroupNameLengthValid() {
        return !groupNameProvided || groupName == null
                || (groupName.strip().length() >= 2 && groupName.strip().length() <= 20);
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

    @AssertTrue(message = "정원은 4명 이상 20명 이하여야 합니다.")
    public boolean isMaxMembersRangeValid() {
        return !maxMembersProvided || maxMembers == null || (maxMembers >= 4 && maxMembers <= 20);
    }

    @AssertTrue(message = "월 회비는 null일 수 없습니다.")
    public boolean isDuesAmountNotNull() {
        return !duesAmountProvided || duesAmount != null;
    }

    @AssertTrue(message = "월 회비는 10,000원 이상 1,000,000원 이하여야 합니다.")
    public boolean isDuesAmountRangeValid() {
        return !duesAmountProvided || duesAmount == null || (duesAmount >= 10_000 && duesAmount <= 1_000_000);
    }

    @AssertTrue(message = "납부일은 null일 수 없습니다.")
    public boolean isDuesDayNotNull() {
        return !duesDayProvided || duesDay != null;
    }

    @AssertTrue(message = "납부일은 1일 이상 28일 이하여야 합니다.")
    public boolean isDuesDayRangeValid() {
        return !duesDayProvided || duesDay == null || (duesDay >= 1 && duesDay <= 28);
    }
}
