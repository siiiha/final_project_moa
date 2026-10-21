package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SavingGroupSettingsAuthorityRow {
    private Long savingGroupId;
    private Long groupId;
    private Long groupLeaderMemberId;
    private Long ownerMemberId;
    private String status;
}
