package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 개설 신청의 계주 대기 관계 생성과 관리자 승인·반려 상태 전환을 DB 없이 검증한다.
class SavingGroupCreationServiceImplTest {

    @Mock
    private SavingGroupMapper savingGroupMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private SavingGroupCreationServiceImpl savingGroupCreationService;

    @Test
    void applyForCreationAddsPendingOwnerMembership() {
        SavingGroupCreationRequest request = creationRequest();
        when(savingGroupMapper.selectRegionIdByName("서울")).thenReturn(1L);
        when(savingGroupMapper.existsMember(10L)).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<SavingGroupCreationRequest>getArgument(0).setGroupId(100L);
            return 1;
        }).when(savingGroupMapper).insertGroups(any(SavingGroupCreationRequest.class));
        when(savingGroupMapper.insertSavingGroup(request)).thenReturn(1);
        when(savingGroupMapper.insertSavingGroupCreationOwnerMembership(100L, 10L)).thenReturn(1);
        when(savingGroupMapper.insertSavingGroupCreationApproval(100L)).thenReturn(1);

        assertThat(savingGroupCreationService.applyForCreationSavingGroup(request)).isTrue();

        verify(savingGroupMapper).insertSavingGroupCreationOwnerMembership(100L, 10L);
    }

    @Test
    void approveCreationChangesOwnerMembershipToApproved() {
        SavingGroupCreationApplicationDetailResponse application = pendingApplication(100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.approveSavingGroupCreationApplication(1L)).thenReturn(1);
        when(savingGroupMapper.publishApprovedSavingGroup(100L)).thenReturn(1);
        when(savingGroupMapper.approveSavingGroupCreationOwnerMembership(100L, 20L)).thenReturn(1);

        savingGroupCreationService.approveCreationApplication(1L, 20L);

        verify(savingGroupMapper).approveSavingGroupCreationOwnerMembership(100L, 20L);
    }

    @Test
    void rejectCreationChangesOwnerMembershipToRejected() {
        SavingGroupCreationApplicationDetailResponse application = pendingApplication(100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.rejectSavingGroupCreationApplication(1L, "사유")).thenReturn(1);
        when(savingGroupMapper.rejectSavingGroupCreationOwnerMembership(100L)).thenReturn(1);

        savingGroupCreationService.rejectCreationApplication(1L, "사유");

        verify(savingGroupMapper).rejectSavingGroupCreationOwnerMembership(100L);
    }

    private SavingGroupCreationRequest creationRequest() {
        SavingGroupCreationRequest request = new SavingGroupCreationRequest();
        request.setMemberId(10L);
        request.setGroupName("테스트 계모임");
        request.setRegionName("서울");
        request.setMaxMembers(10);
        request.setDescription("계모임 소개");
        request.setGroupType("친목계");
        request.setDuesAmount(new BigDecimal("10000"));
        request.setDuesDay(10);
        return request;
    }

    private SavingGroupCreationApplicationDetailResponse pendingApplication(Long groupId) {
        SavingGroupCreationApplicationDetailResponse application = new SavingGroupCreationApplicationDetailResponse();
        application.setGroupId(groupId);
        application.setStatus("PENDING");
        return application;
    }
}
