package com.nightstudy.moa.savingGroup.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SavingGroupSettingChangeScheduler {
    private final SavingGroupService savingGroupService;

    public SavingGroupSettingChangeScheduler(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void applyDueSettingChanges() {
        savingGroupService.applyDueSettingChanges();
    }
}
