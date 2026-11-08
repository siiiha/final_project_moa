package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 내부 대역 데이터만 사용해 유형별 요청자·계주·상태 및 승인에만 적용되는 조건을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupPayoutDecisionServiceTest {
    @Mock SavingGroupMapper mapper;
    @Mock NotificationService notificationService;
    @InjectMocks SavingGroupServiceImpl service;
    SavingGroupPayoutDecisionRow group, request;
    void group(String type, String status) {
        group = new SavingGroupPayoutDecisionRow(); group.setGroupId(20L); group.setOwnerMemberId(30L);
        group.setGroupType(type); group.setStatus(status);
        when(mapper.selectPayoutDecisionGroupForUpdate(10L)).thenReturn(group);
    }
    SavingGroupMembershipRelationRow relation(String role, String state) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(state); return row;
    }
    void owner() { when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(relation("OWNER", "APPROVED")); }
    void request(String status) {
        request = new SavingGroupPayoutDecisionRow(); request.setRequesterId(50L); request.setStatus(status);
        request.setRoundNumber(2); request.setHasEvidence(true);
        when(mapper.selectPayoutDecisionRequestForUpdate(20L, 40L, group.getGroupType())).thenReturn(request);
    }
    void applicant(String role, String state) {
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 50L)).thenReturn(relation(role, state));
    }
    void liveApplicant() { when(mapper.selectSavingGroupJoinMemberForUpdate(50L)).thenReturn(new SavingGroupJoinMemberRow()); }
    void setup(String type) { group(type, "01"); owner(); request("PENDING"); }
    void act(boolean approve) {
        if (approve) service.approveSavingGroupPayoutRequest(10L, 40L, 30L);
        else service.rejectSavingGroupPayoutRequest(10L, 40L, 30L, "  확인 필요  ");
    }
    void fail(String code, boolean approve) {
        assertThat(catchThrowableOfType(() -> act(approve), ApiException.class).getCode()).isEqualTo(code);
        verify(mapper, never()).updatePayoutDecision(any(), any(), any(), any(), any(), any());
    }
    @ParameterizedTest @CsvSource({"순번계,MANAGER,true", "순번계,MEMBER,true", "친목계,MANAGER,true",
            "순번계,MEMBER,false", "친목계,MANAGER,false", "순번계,OWNER,true", "친목계,OWNER,true"})
    void eligibleApplicantCanBeReviewed(String type, String role, boolean approve) {
        setup(type); applicant(role, "APPROVED"); liveApplicant();
        if (approve && type.equals("순번계")) when(mapper.selectPayoutDecisionSequence(20L, 50L)).thenReturn(2);
        when(mapper.updatePayoutDecision(20L, 40L, type, approve ? "APPROVED" : "REJECTED", 30L,
                approve ? null : "확인 필요")).thenReturn(1);
        act(approve);
        verify(mapper).updatePayoutDecision(20L, 40L, type, approve ? "APPROVED" : "REJECTED", 30L, approve ? null : "확인 필요");
        verify(notificationService).send(50L,
                approve ? NotificationType.SAVING_GROUP_PAYOUT_APPROVED : NotificationType.SAVING_GROUP_PAYOUT_REJECTED,
                approve ? "계금·지출 요청이 승인되었습니다." : "계금·지출 요청이 반려되었습니다.",
                "/saving-groups/10");
    }
    @ParameterizedTest @ValueSource(strings = {"순번계", "친목계"})
    void ownerMayReviewOwnRequest(String type) {
        setup(type); request.setRequesterId(30L);
        when(mapper.selectSavingGroupJoinMemberForUpdate(30L)).thenReturn(new SavingGroupJoinMemberRow());
        when(mapper.updatePayoutDecision(20L, 40L, type, "REJECTED", 30L, "확인 필요")).thenReturn(1);
        act(false);
    }
    @ParameterizedTest @CsvSource({"MANAGER,APPROVED", "MEMBER,APPROVED", "OWNER,LEFT", "OWNER,PENDING", "OWNER,EXPELLED"})
    void nonActiveOwnerCannotReview(String role, String status) {
        group("순번계", "01");
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(relation(role, status));
        fail("SAVING_GROUP_OWNER_REQUIRED", true); fail("SAVING_GROUP_OWNER_REQUIRED", false);
    }
    @Test void nonMemberCannotReview() { group("순번계", "01"); fail("SAVING_GROUP_OWNER_REQUIRED", true); }
    @Test void otherGroupOwnerCannotReview() {
        group("순번계", "01"); owner(); group.setOwnerMemberId(999L); fail("SAVING_GROUP_OWNER_REQUIRED", false);
    }
    @Test void loginRequired() {
        assertThat(catchThrowableOfType(() -> service.approveSavingGroupPayoutRequest(10L, 40L, null), ApiException.class).getStatus().value()).isEqualTo(401);
        verifyNoInteractions(mapper);
    }
    @Test void missingGroup() { fail("NOT_FOUND", true); }
    @Test void missingOrForeignRequest() { group("순번계", "01"); owner(); fail("PAYOUT_REQUEST_NOT_FOUND", true); }
    @ParameterizedTest @ValueSource(strings = {"APPROVED", "REJECTED", "COMPLETED", "CANCELLED"})
    void processedRequestCannotBeReviewed(String status) {
        group("순번계", "01"); owner(); request(status);
        fail("PAYOUT_REQUEST_ALREADY_PROCESSED", true); fail("PAYOUT_REQUEST_ALREADY_PROCESSED", false);
    }
    @ParameterizedTest @ValueSource(strings = {"00", "03", "04"})
    void inactiveGroup(String status) { group("순번계", status); fail("SAVING_GROUP_ACTIVITY_RESTRICTED", true); }
    @Test void runningGroupAllowed() {
        setup("친목계"); group.setStatus("02"); applicant("MANAGER", "APPROVED"); liveApplicant();
        when(mapper.updatePayoutDecision(20L, 40L, "친목계", "APPROVED", 30L, null)).thenReturn(1); act(true);
    }
    @ParameterizedTest @ValueSource(strings = {"LEFT", "EXPELLED", "PENDING", "REJECTED"})
    void formerApplicantCannotBeReviewed(String status) {
        setup("순번계"); applicant("MEMBER", status); fail("PAYOUT_REQUESTER_INELIGIBLE", true); fail("PAYOUT_REQUESTER_INELIGIBLE", false);
    }
    @Test void socialMemberCannotRequest() {
        setup("친목계"); applicant("MEMBER", "APPROVED"); fail("PAYOUT_REQUESTER_INELIGIBLE", true);
    }
    @Test void deletedApplicantCannotBeReviewed() {
        setup("순번계"); applicant("MEMBER", "APPROVED"); fail("PAYOUT_REQUESTER_INELIGIBLE", false);
    }
    @Test void sequenceMismatchBlocksApprovalButNotRejection() {
        setup("순번계"); applicant("MEMBER", "APPROVED"); liveApplicant();
        when(mapper.selectPayoutDecisionSequence(20L, 50L)).thenReturn(3); fail("PAYOUT_SEQUENCE_MISMATCH", true);
        when(mapper.updatePayoutDecision(20L, 40L, "순번계", "REJECTED", 30L, "확인 필요")).thenReturn(1); act(false);
    }
    @Test void missingSequenceCannotApprove() {
        setup("순번계"); applicant("MEMBER", "APPROVED"); liveApplicant(); fail("PAYOUT_SEQUENCE_MISMATCH", true);
    }
    @Test void missingEvidenceBlocksApprovalButNotRejection() {
        setup("친목계"); applicant("MANAGER", "APPROVED"); liveApplicant(); request.setHasEvidence(false);
        fail("PAYOUT_EVIDENCE_REQUIRED", true);
        when(mapper.updatePayoutDecision(20L, 40L, "친목계", "REJECTED", 30L, "확인 필요")).thenReturn(1); act(false);
    }
    @Test void conditionalUpdateConflict() {
        setup("친목계"); applicant("MANAGER", "APPROVED"); liveApplicant();
        assertThat(catchThrowableOfType(() -> act(true), ApiException.class).getStatus().value()).isEqualTo(409);
    }
    @ParameterizedTest @ValueSource(strings = {"", "   ", "\t\n"})
    void blankReason(String reason) {
        assertThat(catchThrowableOfType(() -> service.rejectSavingGroupPayoutRequest(10L, 40L, 30L, reason), ApiException.class).getStatus().value()).isEqualTo(400);
        verifyNoInteractions(mapper);
    }
    @Test void longReason() {
        assertThat(catchThrowableOfType(() -> service.rejectSavingGroupPayoutRequest(10L, 40L, 30L, "가".repeat(501)), ApiException.class).getStatus().value()).isEqualTo(400);
    }
}
