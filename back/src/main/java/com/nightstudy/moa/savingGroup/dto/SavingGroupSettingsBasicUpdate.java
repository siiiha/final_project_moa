package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingGroupSettingsBasicUpdate {
    private final Long groupId;
    private final String groupName;
    private final String description;
    private final Integer maxMembers;
}
