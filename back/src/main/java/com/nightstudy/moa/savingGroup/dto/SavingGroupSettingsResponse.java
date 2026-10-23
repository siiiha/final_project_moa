package com.nightstudy.moa.savingGroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingGroupSettingsResponse {
    private final Long savingGroupId;
    private final String status;
    private final boolean canAccessSettings;
    private final boolean canEditSettings;
    private final SavingGroupCurrentSettingsResponse current;
    private final SavingGroupPendingSettingsResponse pending;
    private final List<String> editableFields;
}
