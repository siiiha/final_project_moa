package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinTargetRow;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 계주의 삭제 신청 권한과 중복 신청 방지를 DB 없이 검증한다.
class SavingGroupDeletionApplicationServiceTest {

    @Mock
    private SavingGroupMapper savingGroupMapper;

    @InjectMocks
    private SavingGroupServiceImpl savingGroupService;

    @Test
    void ownerCanRequestDeletion() {
        when(savingGroupMapper.selectSavingGroupDeleteTargetForUpdate(10L)).thenReturn(target("01", 20L));
        when(savingGroupMapper.insertSavingGroupDeletionApplication(30L, "운영 종료")).thenReturn(1);

        savingGroupService.requestSavingGroupDeletion(10L, 20L, " 운영 종료 ");

        verify(savingGroupMapper).insertSavingGroupDeletionApplication(30L, "운영 종료");
    }

    @Test
    void nonOwnerCannotRequestDeletion() {
        when(savingGroupMapper.selectSavingGroupDeleteTargetForUpdate(10L)).thenReturn(target("01", 20L));

        ApiException exception = catchThrowableOfType(
                () -> savingGroupService.requestSavingGroupDeletion(10L, 21L, "운영 종료"), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_OWNER_REQUIRED");
        verify(savingGroupMapper, never()).insertSavingGroupDeletionApplication(30L, "운영 종료");
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        when(savingGroupMapper.selectSavingGroupDeleteTargetForUpdate(10L)).thenReturn(target("01", 20L));
        when(savingGroupMapper.countPendingSavingGroupDeletionApplication(30L)).thenReturn(1);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupService.requestSavingGroupDeletion(10L, 20L, "운영 종료"), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_DELETION_ALREADY_PENDING");
        verify(savingGroupMapper, never()).insertSavingGroupDeletionApplication(30L, "운영 종료");
    }

    private SavingGroupJoinTargetRow target(String status, Long ownerId) {
        SavingGroupJoinTargetRow target = new SavingGroupJoinTargetRow();
        target.setSavingGroupId(10L);
        target.setGroupId(30L);
        target.setStatus(status);
        target.setOwnerMemberId(ownerId);
        return target;
    }
}
