package com.nightstudy.moa.savingGroup.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinTargetRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMembershipRelationRow;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 계모임 삭제 성공 후 계주를 제외한 기존 승인 회원에게만 삭제 결과를 알리는지 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupDeletionNotificationServiceTest {

    @Mock private SavingGroupMapper savingGroupMapper;
    @Mock private NotificationService notificationService;
    @InjectMocks private SavingGroupServiceImpl savingGroupService;

    @Test
    void deletionNotifiesApprovedMembersExceptOwner() {
        SavingGroupJoinTargetRow target = new SavingGroupJoinTargetRow();
        target.setSavingGroupId(10L);
        target.setGroupId(20L);
        target.setOwnerMemberId(30L);
        target.setStatus("01");
        SavingGroupMembershipRelationRow owner = new SavingGroupMembershipRelationRow();
        owner.setMemberRole("OWNER");
        owner.setMembershipStatus("APPROVED");
        when(savingGroupMapper.selectSavingGroupDeleteTargetForUpdate(10L)).thenReturn(target);
        when(savingGroupMapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(owner);
        when(savingGroupMapper.selectApprovedSavingGroupMemberIds(20L)).thenReturn(List.of(30L, 40L, 50L));
        when(savingGroupMapper.softDeleteSavingGroup(20L)).thenReturn(1);

        savingGroupService.deleteSavingGroup(10L, 30L);

        verify(notificationService, never()).send(eq(30L), any(), any(), any());
        verify(notificationService).send(40L, NotificationType.SAVING_GROUP_DELETED,
                "참여 중인 계모임이 삭제되었습니다.", null);
        verify(notificationService).send(50L, NotificationType.SAVING_GROUP_DELETED,
                "참여 중인 계모임이 삭제되었습니다.", null);
    }
}
