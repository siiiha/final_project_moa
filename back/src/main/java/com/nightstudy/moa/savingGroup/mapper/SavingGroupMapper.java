package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 목록 조회는 최대 세 번의 SELECT로 처리한다.
@Mapper
public interface SavingGroupMapper {
    // AI작성: 대상 모임과 월 범위를 함께 제한해 공유 일정을 조회한다.
    List<SavingGroupCalendarDto.EventRow> selectCalendarEvents(@Param("groupId") Long groupId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    int insertCalendarEvent(SavingGroupCalendarDto.Insert event);
    // AI작성: 같은 계모임에 속한 일정만 삭제한다.
    int deleteCalendarEvent(@Param("groupId") Long groupId, @Param("scheduleId") Long scheduleId);
    // AI작성: 등록 대상 모임에서 선택일에 이미 저장된 일정 수를 집계한다.
    int countCalendarEvents(@Param("groupId") Long groupId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // AI작성: 공용 계좌에 속한 내부 장부만 조회한다.
    // AI작성: 월별 입금 집계와 회비 기준 조회에 선택 월의 범위를 전달한다.
    int countSavingGroupPaymentStatus(@Param("groupId") Long groupId);
    List<SavingGroupPaymentStatusRow> selectSavingGroupPaymentStatus(@Param("groupId") Long groupId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
            @Param("offset") long offset, @Param("size") int size);
    java.math.BigDecimal selectMonthlyPaymentDues(@Param("groupId") Long groupId, @Param("end") LocalDateTime end);
    // AI작성: 납부일·대상 월·알림 구분을 함께 전달해 미납자의 중복 알림을 제외한다.
    List<SavingGroupDuesNotificationTarget> selectDuesNotificationTargets(
            @Param("duesDay") int duesDay,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("paymentMonth") String paymentMonth,
            @Param("reminderKey") String reminderKey);

    int selectSavingGroupTransactionCount(@Param("accountId") Long accountId);
    List<SavingGroupTransactionRow> selectSavingGroupTransactions(@Param("accountId") Long accountId,
            @Param("offset") long offset, @Param("size") int size);
    // AI작성: 공통 모임 잠금 아래 예약 목록을 통째로 저장하며 실제 회차·지급 원장은 변경하지 않는다.
    SavingGroupTurnOrderPlanRow selectTurnOrderGroupForUpdate(@Param("savingGroupId") Long savingGroupId);
    List<SavingGroupTurnOrderAssignmentRow> selectTurnOrderAssignmentMembers(@Param("groupId") Long groupId);
    SavingGroupTurnOrderPlanRow selectTurnOrderPlan(@Param("savingGroupId") Long savingGroupId,
            @Param("month") java.time.LocalDate month);
    List<SavingGroupTurnOrderAssignmentRow> selectTurnOrderPlanMembers(@Param("planId") Long planId);
    int insertTurnOrderPlan(SavingGroupTurnOrderPlanRow plan);
    int deleteTurnOrderPlanMembers(@Param("planId") Long planId);
    int insertTurnOrderPlanMember(@Param("planId") Long planId, @Param("memberId") Long memberId,
            @Param("roundNumber") int roundNumber);
    int updateTurnOrderSequence(@Param("groupId") Long groupId, @Param("memberId") Long memberId,
            @Param("roundNumber") int roundNumber);
    Long selectTurnOrderSavingGroupId(@Param("groupId") Long groupId);
    boolean existsCompletedTurnOrderPayout(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
    boolean existsUpcomingTurnOrderPayout(@Param("groupId") Long groupId, @Param("startRound") int startRound);
    // AI작성: 추천에는 현재 승인된 모든 역할을 포함하며 저장 SQL은 추가하지 않는다.
    SavingGroupTurnOrderContextRow selectTurnOrderRecommendationContext(@Param("savingGroupId") Long savingGroupId);
    List<SavingGroupTurnOrderMemberRow> selectTurnOrderRecommendationMembers(@Param("groupId") Long groupId);
    // AI작성: 모임 잠금 후 현재 읽기로 재전송·최초 5분 기준·확인 소비를 직렬화한다.
    Long selectDepositAccountForUpdate(@Param("groupId") Long groupId);
    LocalDateTime selectDepositNow();
    SavingGroupDepositRow selectDepositRequestForUpdate(@Param("memberId") Long memberId,
            @Param("requestKey") String requestKey);
    SavingGroupDepositRow selectDepositAnchorForUpdate(@Param("accountId") Long accountId,
            @Param("memberId") Long memberId, @Param("amount") java.math.BigDecimal amount,
            @Param("since") LocalDateTime since);
    int insertDepositTransaction(SavingGroupDepositRow row);
    int insertDepositRequest(SavingGroupDepositRow row);
    int refreshDepositConfirmation(SavingGroupDepositRow row);
    int completeDepositRequest(SavingGroupDepositRow row);
    // AI작성: 모임 잠금 후 현재 읽기로 승인된 회계 목록을 확인해 동시 지정 시 최대 한 명을 보장한다.
    List<Long> selectSavingGroupManagerIdsForUpdate(@Param("groupId") Long groupId);
    int updateSavingGroupMemberRole(@Param("groupId") Long groupId, @Param("crewMemberId") Long crewMemberId,
            @Param("previousRole") String previousRole, @Param("role") String role);
    // AI작성: 계주를 제외한 승인 계원만 같은 조건으로 집계·조회한다. 집계도 최대 20명이다.
    int selectSavingGroupMemberCount(@Param("groupId") Long groupId, @Param("keywordPattern") String keywordPattern);
    List<SavingGroupMemberResponse> selectSavingGroupMembers(
            @Param("groupId") Long groupId, @Param("keywordPattern") String keywordPattern, @Param("offset") int offset);
    // AI작성: 모임 잠금으로 같은 모임의 중복 검사와 저장을 직렬화한다. DB 컬럼 적용 후 사용한다.
    SavingGroupPayoutTargetRow selectPayoutTargetForUpdate(@Param("groupId") Long groupId);
    // AI작성: 지급 요청을 실제 심사할 수 있는 승인 계주의 회원 번호만 조회한다.
    List<Long> selectSavingGroupPayoutReviewerIds(@Param("groupId") Long groupId);
    Integer selectPayoutSequence(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
    SavingGroupPayoutRoundRow selectPayoutRoundForUpdate(@Param("groupId") Long groupId,
            @Param("roundNumber") int roundNumber);
    boolean existsBlockingRoundPayout(@Param("roundId") Long roundId);
    boolean existsBlockingReceiptExpense(@Param("groupId") Long groupId, @Param("hash") String hash);
    int insertRoundPayout(SavingGroupPayoutInsertRow row);
    int insertReceiptExpense(SavingGroupPayoutInsertRow row);
    String selectPayoutReceiptForOwner(@Param("groupId") Long groupId, @Param("expenseId") Long expenseId);
    // AI작성: 저장된 모임 유형만 사용하고 유형별 목록과 집계는 같은 필터를 공유한다.
    String selectSavingGroupPayoutType(@Param("groupId") Long groupId);
    int selectSavingGroupPayoutRequestCount(@Param("groupId") Long groupId,
            @Param("groupType") String groupType, @Param("status") String status);
    List<SavingGroupPayoutRequestResponse> selectSavingGroupPayoutRequests(@Param("groupId") Long groupId,
            @Param("groupType") String groupType, @Param("status") String status, @Param("offset") int offset);
    // AI작성: 모임→요청→회원 잠금 후 대기 상태 한 건만 심사하며 자금 이동은 수행하지 않는다.
    SavingGroupPayoutDecisionRow selectPayoutDecisionGroupForUpdate(@Param("savingGroupId") Long savingGroupId);
    SavingGroupPayoutDecisionRow selectPayoutDecisionRequestForUpdate(@Param("groupId") Long groupId,
            @Param("requestId") Long requestId, @Param("groupType") String groupType);
    Integer selectPayoutDecisionSequence(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
    int updatePayoutDecision(@Param("groupId") Long groupId, @Param("requestId") Long requestId,
            @Param("groupType") String groupType, @Param("status") String status,
            @Param("reviewerId") Long reviewerId, @Param("rejectReason") String rejectReason);
    // AI작성: 인증된 작성자의 미삭제 소속 공지만 논리 삭제하고 영향 행 수를 반환한다.
    int deleteSavingGroupNotice(@Param("groupId") Long groupId, @Param("noticeId") Long noticeId,
            @Param("memberId") Long memberId);
    // AI작성: 기존 상세 DTO를 재사용하되 변경 전 공지를 잠그고 미삭제 소속 공지만 수정한다.
    SavingGroupNoticeDetailResponse selectSavingGroupNoticeForUpdate(
            @Param("groupId") Long groupId, @Param("noticeId") Long noticeId);
    int updateSavingGroupNotice(@Param("groupId") Long groupId, @Param("noticeId") Long noticeId,
            @Param("title") String title, @Param("content") String content);
    // AI작성: 공개 상세 조회에서는 행 잠금 없이 본인의 신청 조건만 읽는다.
    SavingGroupJoinMemberRow selectSavingGroupJoinMember(@Param("memberId") Long memberId);
    SavingGroupMembershipRelationRow selectSavingGroupMembership(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);
    long selectApprovedSavingGroupCountReadOnly(@Param("memberId") Long memberId);

    // AI작성: 계모임 개설 기능도 동일 도메인 Mapper와 XML namespace를 사용하도록 통합한다.
    int insertGroups(SavingGroupCreationRequest savingGroupCreationRequest);
    int insertSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest);
    // AI작성: 계모임 회원 상태와 채팅방 참여 상태를 같은 트랜잭션에서 반영한다.
    int insertSavingGroupChatRoom(@Param("groupId") Long groupId);
    boolean canAccessSavingGroupChat(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
    int insertSavingGroupChatMember(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
    int deleteSavingGroupChatMember(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    int insertSavingGroupCreationOwnerMembership(@Param("groupId") Long groupId,
                                                  @Param("memberId") Long memberId);
    int insertSavingGroupCreationApproval(@Param("groupId") Long groupId);
    Long selectRegionIdByName(@Param("regionName") String regionName);
    // AI작성: 기존 지역 DTO로 구·동 소속 정보를 조회하며 중복 지역명도 서비스에서 검증한다.
    List<com.nightstudy.moa.member.dto.region.MemberRegionDto> selectCreationRegionsByName(
            @Param("regionName") String regionName);
    boolean existsMember(@Param("memberId") Long memberId);
    int selectSavingGroupCreationApplicationCount(SavingGroupCreationApplicationListCondition condition);
    List<SavingGroupCreationApplicationListItemResponse> selectSavingGroupCreationApplications(
            SavingGroupCreationApplicationListCondition condition);
    SavingGroupCreationApplicationDetailResponse selectSavingGroupCreationApplication(
            @Param("approvalId") Long approvalId);
    SavingGroupCreationApplicationDetailResponse selectSavingGroupCreationApplicationForUpdate(
            @Param("approvalId") Long approvalId);
    int approveSavingGroupCreationApplication(@Param("approvalId") Long approvalId);
    int rejectSavingGroupCreationApplication(@Param("approvalId") Long approvalId,
                                             @Param("rejectReason") String rejectReason);
    int publishApprovedSavingGroup(@Param("groupId") Long groupId);
    int approveSavingGroupCreationOwnerMembership(@Param("groupId") Long groupId,
                                                   @Param("approvedBy") Long approvedBy);
    int rejectSavingGroupCreationOwnerMembership(@Param("groupId") Long groupId);

    // AI작성: 권한 검증으로 확인한 내부 groupId를 사용하고 같은 조건으로 개수와 페이지를 조회한다.
    int selectSavingGroupJoinRequestCount(@Param("groupId") Long groupId, @Param("status") String status);
    List<SavingGroupJoinRequestRow> selectSavingGroupJoinRequests(@Param("groupId") Long groupId,
            @Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);

    // 목록 검색 조건에 맞는 공개 계모임의 총 개수를 조회한다.
    // AI작성: 일대다 데이터를 분리 조회하여 공지 수가 계원 집계에 영향을 주지 않게 한다.
    // AI작성: 상세 URL의 계모임 ID로 두 식별값을 조회한다.
    SavingGroupRow selectSavingGroupDetailBySavingGroupId(@Param("savingGroupId") Long savingGroupId);

    SavingGroupRow selectSavingGroupDetail(@Param("groupId") Long groupId);
    long selectDetailMemberCount(@Param("groupId") Long groupId);
    List<SavingGroupNoticeSummaryResponse> selectRecentNotices(@Param("groupId") Long groupId);
    // AI작성: 모임 존재 여부는 Service에서 확인하며 고정 공지를 제외한 일반 공지 개수를 조회한다.
    int selectSavingGroupNoticeCount(@Param("groupId") Long groupId);
    // AI작성: Service에서 검증한 정렬과 PageMeta의 offset으로 공지 요약을 최대 4개 조회한다.
    List<SavingGroupNoticeSummaryResponse> selectSavingGroupNotices(
            @Param("groupId") Long groupId, @Param("sort") String sort, @Param("offset") int offset);
    // AI작성: 모든 페이지 상단의 고정 공지는 기존 공지 요약 DTO로 별도 조회한다.
    List<SavingGroupNoticeSummaryResponse> selectSavingGroupPinnedNotices(@Param("groupId") Long groupId);
    // AI작성: 고정 변경은 이 모임 잠금 후 기존 회원 관계 잠금·공지 잠금 순서로 수행해 동시 지정을 직렬화한다.
    SavingGroupJoinTargetRow selectSavingGroupNoticeTargetForUpdate(@Param("savingGroupId") Long savingGroupId);
    SavingGroupNoticePinRow selectSavingGroupNoticePinForUpdate(
            @Param("groupId") Long groupId, @Param("noticeId") Long noticeId);
    // AI작성: 집계 대신 현재 읽기의 고정 ID 목록 크기를 사용해 이전 스냅샷으로 인한 두 개 제한 초과를 방지한다.
    List<Long> selectSavingGroupPinnedNoticeIdsForUpdate(@Param("groupId") Long groupId);
    // AI작성: Service의 권한·개수 검증 이후 대상 모임의 공지만 변경하고 영향 행 수를 반환한다.
    int updateSavingGroupNoticePinned(@Param("groupId") Long groupId, @Param("noticeId") Long noticeId,
            @Param("pinned") boolean pinned);
    // AI작성: 공통 groupId와 공지 ID가 모두 일치하는 상세만 조회하며 권한은 Service에서 검증한다.
    SavingGroupNoticeDetailResponse selectSavingGroupNoticeDetail(
            @Param("groupId") Long groupId, @Param("noticeId") Long noticeId);
    // AI작성: Service에서 모임 상태와 작성 권한을 검증한 공지를 저장하고 영향 행 수를 반환한다.
    int insertSavingGroupNotice(SavingGroupNoticeCreateRow notice);
    // AI작성: 공지 등록 알림 대상인 승인 계주·운영진·계원의 회원 번호를 중복 없이 조회한다.
    List<Long> selectApprovedSavingGroupMemberIds(@Param("groupId") Long groupId);
    List<String> selectApprovedMemberRoles(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 신청 ID·모임 ID·PENDING 조건이 일치하는 기존 행만 승인하고 영향 행 수를 반환한다.
    int approveSavingGroupJoinRequest(@Param("crewMemberId") Long crewMemberId,
            @Param("groupId") Long groupId, @Param("approvedBy") Long approvedBy);

    // AI작성: 신청 ID·모임 ID·PENDING 조건이 일치하는 기존 행만 거절하고 영향 행 수를 반환한다.
    int rejectSavingGroupJoinRequest(@Param("crewMemberId") Long crewMemberId,
            @Param("groupId") Long groupId);

    // AI작성: 회원 잠금 순서를 지키기 위한 식별 조회이며 검증 결과로 사용하지 않는다.
    SavingGroupJoinApprovalTargetRow selectSavingGroupJoinApprovalTarget(@Param("crewMemberId") Long crewMemberId);

    // AI작성: 회원과 계모임 잠금 이후 신청 행의 소속과 상태를 다시 확인한다.
    SavingGroupJoinApprovalTargetRow selectSavingGroupJoinApprovalTargetForUpdate(@Param("crewMemberId") Long crewMemberId);

    int selectSavingGroupCount(SavingGroupListCondition condition);

    // 목록 검색 조건에 맞는 공개 계모임의 기본 정보를 조회한다.
    List<SavingGroupRow> selectSavingGroupList(SavingGroupListCondition condition);

    // 전달받은 모임 ID별 승인 계원 수를 조회한다.
    List<SavingGroupMemberCount> selectApprovedMemberCounts(@Param("groupIds") List<Long> groupIds);

    // AI작성: 가입 신청 전 회원의 계정 상태와 본인 인증 완료 여부를 잠근 상태로 조회한다.
    SavingGroupJoinMemberRow selectSavingGroupJoinMemberForUpdate(@Param("memberId") Long memberId);

    // AI작성: 가입 신청 대상의 내부 모임 ID, 상태, 정원을 잠근 상태로 조회한다.
    SavingGroupJoinTargetRow selectSavingGroupJoinTargetForUpdate(@Param("savingGroupId") Long savingGroupId);

    // AI작성: 동일 회원의 기존 가입·신청 이력을 잠근 상태로 조회한다.
    SavingGroupMembershipRelationRow selectSavingGroupMembershipForUpdate(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 정원 판단에는 승인된 계원만 포함하며 대기·탈퇴 회원은 제외한다.
    long selectApprovedMemberCountForUpdate(@Param("groupId") Long groupId);

    // AI작성: 승인된 실제 가입만 집계하고 종료·삭제 모임은 최대 가입 수에서 제외한다.
    long selectApprovedSavingGroupCount(@Param("memberId") Long memberId);

    // AI작성: 추가 입력 없이 인증 회원 본인의 가입 신청을 대기 상태로 저장한다.
    int insertSavingGroupJoinRequest(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 자진 탈퇴한 회원만 기존 관계 행을 대기 신청 상태로 되돌린다.
    int reactivateLeftSavingGroupMembership(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // 계모임 설정 화면의 접근 권한과 상태를 확인하기 위해 계주 정보를 조회한다.
    SavingGroupSettingsAuthorityRow selectSavingGroupSettingsAuthority(
            @Param("savingGroupId") Long savingGroupId);

    // 계모임 설정 화면에 표시할 현재 적용 중인 정보를 조회한다.
    SavingGroupCurrentSettingsResponse selectSavingGroupCurrentSettings(
            @Param("savingGroupId") Long savingGroupId);

    // 수정 대상 계모임 행을 잠근 뒤 존재 여부, 계주 권한, 상태를 조회한다.
    SavingGroupSettingsAuthorityRow selectSavingGroupSettingsAuthorityForUpdate(
            @Param("savingGroupId") Long savingGroupId);

    // 승인된 OWNER 계원 행을 잠근 뒤 계주 회원 ID를 조회한다.
    Long selectSavingGroupOwnerMemberIdForUpdate(@Param("groupId") Long groupId);

    // 요청으로 전달된 이름, 소개글, 정원 필드만 기본 모임 정보에 수정한다.
    int updateSavingGroupBasicSettings(@Param("update") SavingGroupSettingsBasicUpdate update);

    // 요청으로 전달된 회비와 납부일 필드를 현재 계모임 설정에 즉시 수정한다.
    int updateSavingGroupCurrentDues(@Param("update") SavingGroupCurrentDuesUpdate update);

    // 다음 달 적용할 회비와 납부일을 저장하고 같은 적용일의 예정값은 전달된 필드만 갱신한다.
    int upsertSavingGroupSettingChange(@Param("change") SavingGroupSettingChangeRow change);

    // 적용 시각이 지난 PENDING 예정 변경 후보를 조회한다.
    List<SavingGroupSettingChangeRow> selectDueSettingChanges(@Param("appliedAt") LocalDateTime appliedAt);

    // 예정값 반영 전에 계모임 상태를 잠근 뒤 조회한다.
    String selectSavingGroupStatusForUpdate(@Param("groupId") Long groupId);

    // PENDING 상태이면서 적용 시각이 지난 예정 변경 행을 잠근 뒤 조회한다.
    SavingGroupSettingChangeRow selectSavingGroupSettingChangeForApply(
            @Param("settingChangeId") Long settingChangeId, @Param("appliedAt") LocalDateTime appliedAt);

    // 예정 변경의 회비와 납부일을 현재 계모임 설정에 반영한다.
    int applySavingGroupSettingChange(@Param("change") SavingGroupSettingChangeRow change);

    // 반영을 완료한 예정 변경 행을 APPLIED 상태와 실제 적용 시각으로 갱신한다.
    int markSavingGroupSettingChangeApplied(
            @Param("settingChangeId") Long settingChangeId, @Param("appliedAt") LocalDateTime appliedAt);

    // 삭제되지 않은 계모임의 상태를 변경한다.
    int updateSavingGroupStatus(
            @Param("groupId") Long groupId, @Param("status") String status);
    // AI작성: URL의 groupId로 계모임과 생성 계주를 잠가 추방 권한 검증에 사용한다.
    SavingGroupJoinTargetRow selectSavingGroupExpulsionTargetForUpdate(@Param("groupId") Long groupId);

    // AI작성: 승인 상태인 기존 계원 관계 한 건만 추방 상태로 조건부 변경한다.
    int expelApprovedSavingGroupMember(@Param("crewMemberId") Long crewMemberId,
                                       @Param("groupId") Long groupId);
    // 정지된 계모임의 아직 반영되지 않은 예정 변경을 HELD 상태로 보류한다.
    int holdPendingSavingGroupSettingChanges(@Param("groupId") Long groupId);

    // 종료된 계모임의 아직 반영되지 않은 예정 변경을 CANCELED 상태로 취소한다.
    int cancelUnappliedSavingGroupSettingChanges(
            @Param("groupId") Long groupId, @Param("canceledAt") LocalDateTime canceledAt);

    // 정지 해제된 계모임의 HELD 예정 변경을 다시 PENDING으로 전환하고 필요하면 적용일을 재설정한다.
    int resumeHeldSavingGroupSettingChanges(
            @Param("groupId") Long groupId,
            @Param("resumedAt") LocalDateTime resumedAt,
            @Param("nextEffectiveAt") LocalDateTime nextEffectiveAt);
    // AI작성: 삭제 요청 시 계모임의 내부 모임 ID·상태·생성 계주를 잠가 존재와 권한을 같은 트랜잭션에서 검증한다.
    SavingGroupJoinTargetRow selectSavingGroupDeleteTargetForUpdate(@Param("savingGroupId") Long savingGroupId);

    // AI작성: 물리 삭제 대신 아직 삭제되지 않은 공통 모임 행에 삭제 시각만 기록한다.
    int softDeleteSavingGroup(@Param("groupId") Long groupId);
    // AI작성: 같은 계모임의 미처리 삭제 심사 중복 여부를 확인한다.
    int countPendingSavingGroupDeletionApplication(@Param("groupId") Long groupId);
    // AI작성: 계주의 필수 삭제 사유를 공통 심사 테이블에 저장한다.
    int insertSavingGroupDeletionApplication(
            @Param("groupId") Long groupId, @Param("reason") String reason);
    // AI작성: 공동계좌 거래의 입금·출금 합계로 현재 공동자금을 계산한다.
    java.math.BigDecimal selectSavingGroupBalance(@Param("groupId") Long groupId);
    // AI작성: 진행 또는 정지 상태의 계모임인지 삭제 승인 직전에 확인한다.
    boolean existsActiveSavingGroup(@Param("groupId") Long groupId);
    // AI작성: 완료·반려되지 않은 출금 요청이 남았는지 확인한다.
    boolean existsPendingPayout(@Param("groupId") Long groupId);
    int approveSavingGroupDeletionApplication(@Param("approvalId") Long approvalId);
    int rejectSavingGroupDeletionApplication(
            @Param("approvalId") Long approvalId, @Param("rejectReason") String rejectReason);
    // AI작성: 자진 탈퇴는 잠금 조회로 확인한 본인의 승인 관계 한 건만 LEFT 상태로 전환한다.
    int leaveApprovedSavingGroupMember(@Param("crewMemberId") Long crewMemberId,
                                       @Param("groupId") Long groupId);
}
