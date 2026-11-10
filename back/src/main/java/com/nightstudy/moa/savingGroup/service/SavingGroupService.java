package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailPageResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinApprovalTargetRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    // AI작성: 승인된 모임원의 공용 계좌 거래내역을 페이지로 조회한다.
    // AI작성: 선택한 월의 입금 합계로 회비 납부 상태를 조회한다.
    com.nightstudy.moa.savingGroup.dto.SavingGroupPaymentStatusListResponse getSavingGroupPaymentStatus(
            Long savingGroupId, Long memberId, java.time.YearMonth month, int page);

    com.nightstudy.moa.savingGroup.dto.SavingGroupTransactionListResponse getSavingGroupTransactions(
            Long savingGroupId, Long memberId, int page);
    // AI작성: 승인 계주에게 전체 승인 회원의 무작위 모의 순번을 제안하며 저장하지 않는다.
    java.util.List<com.nightstudy.moa.savingGroup.dto.SavingGroupTurnOrderRecommendationResponse>
            getTurnOrderRecommendations(Long savingGroupId, Long memberId);
    // AI작성: 승인 모임원에게만 계좌 마스킹 정보와 내부 장부 잔액을 제공한다.
    com.nightstudy.moa.savingGroup.dto.SavingGroupAccountResponse getSavingGroupAccount(Long savingGroupId, Long memberId);
    // AI작성: 승인 계주만 기존 계원의 MEMBER·MANAGER 역할을 변경하며 회계는 최대 한 명이다.
    void updateSavingGroupMemberRole(Long savingGroupId, Long targetMemberId, Long memberId, String role);
    // AI작성: 승인 계주에게만 최대 20명의 계원을 10명 단위로 제공한다.
    com.nightstudy.moa.savingGroup.dto.SavingGroupMemberListResponse getSavingGroupMembers(
            Long savingGroupId, Long memberId, int page);
    // AI작성: 전체 승인 계원에서 이름·원문 이메일을 검색한 뒤 기존 페이지 제한을 적용한다.
    com.nightstudy.moa.savingGroup.dto.SavingGroupMemberListResponse getSavingGroupMembers(
            Long savingGroupId, Long memberId, String keyword, int page);
    // AI작성: 인증·모임·계주 검증 후 저장된 유형에 맞는 요청 목록만 조회한다.
    com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRequestListResponse getSavingGroupPayoutRequests(
            Long savingGroupId, Long memberId, String status, int page);
    // AI작성: 실제 지급 없이 계주의 대기 요청 심사만 처리한다.
    void approveSavingGroupPayoutRequest(Long savingGroupId, Long requestId, Long memberId);
    void rejectSavingGroupPayoutRequest(Long savingGroupId, Long requestId, Long memberId, String rejectReason);
    // AI작성: 현재 승인된 공지 작성자만 소속 공지를 논리 삭제한다.
    void deleteSavingGroupNotice(Long savingGroupId, Long noticeId, Long memberId);
    // AI작성: 공통 groupId 기준으로 승인 운영진인 작성자 본인의 제목·내용 전체 수정을 처리한다.
    Long updateSavingGroupNotice(Long savingGroupId, Long noticeId, Long memberId, String title, String content);
    // AI작성: 승인된 OWNER·MANAGER만 공지 고정을 변경하며 모임당 최대 두 개를 같은 트랜잭션에서 검증한다.
    void updateSavingGroupNoticePinned(Long savingGroupId, Long noticeId, Long memberId, Boolean pinned);
    // AI작성: 공통 groupId에 속한 공지 상세를 해당 모임의 승인된 회원에게만 제공한다.
    SavingGroupNoticeDetailResponse getSavingGroupNoticeDetail(
            Long groupId, Long noticeId, Long memberId);

    // AI작성: 승인된 OWNER·MANAGER만 모집중·진행중 계모임에 공지를 작성한다. 입력 Validation은 Controller에서 수행한다.
    void createSavingGroupNotice(Long savingGroupId, Long memberId, String title, String content);

    // AI작성: memberId는 인증 계층에서 검증된 ID 또는 비로그인의 null만 받는다.
    SavingGroupDetailResponse getSavingGroupDetail(Long savingGroupId, Long memberId);
    // AI작성: 기존 상세 데이터와 공지 전용 PageMeta를 함께 반환하며 공개 조회 권한은 유지한다.
    SavingGroupDetailPageResponse getSavingGroupDetail(
            Long savingGroupId, Long memberId, SavingGroupNoticeSearchCondition condition);
    // AI작성: 승인된 계주에게만 신청 목록을 제공하며 상태·페이지는 서버에서 검증한다.
    SavingGroupJoinRequestListResponse getSavingGroupJoinRequests(
            Long savingGroupId, Long memberId, String status, int page);

    // AI작성: 승인 대상 검증과 기존 신청 행의 상태 변경을 하나의 트랜잭션에서 처리한다.
    void approveSavingGroupJoin(Long savingGroupId, Long crewMemberId, Long memberId);

    // AI작성: 승인된 계주가 대기 중인 가입 신청만 거절 상태로 처리한다.
    void rejectSavingGroupJoin(Long savingGroupId, Long crewMemberId, Long memberId);

    // AI작성: 계주는 자신의 계모임에 승인된 계원의 관계 상태만 추방으로 변경할 수 있다.
    void expelApprovedSavingGroupMember(Long savingGroupId, Long targetMemberId, Long memberId);

    // AI작성: 계주는 승인 후 시작 전인 자신의 계모임만 소프트 삭제할 수 있다.
    void deleteSavingGroup(Long savingGroupId, Long memberId);

    // AI작성: 인증된 계원 본인은 모집중인 계모임의 승인 관계만 탈퇴 상태로 변경할 수 있다.
    void leaveSavingGroup(Long savingGroupId, Long memberId);

    // AI작성: 호출자의 기존 트랜잭션 안에서만 검증하며, 반환값을 별도 트랜잭션의 승인 근거로 사용하지 않는다.
    SavingGroupJoinApprovalTargetRow validateSavingGroupJoinApproval(
            Long savingGroupId, Long crewMemberId, Long memberId);
    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);
    SavingGroupSettingsResponse getSavingGroupSettings(Long savingGroupId, Long loginMemberId);
    void updateSavingGroupSettings(Long savingGroupId, Long loginMemberId, SavingGroupSettingsPatchRequest request);
    void applyDueSettingChanges();
    void changeSavingGroupStatus(Long groupId, String nextStatus);
    // AI작성: 인증된 회원 본인 명의로 계모임 가입 신청을 대기 상태로 등록한다.
    void requestSavingGroupJoin(Long savingGroupId, Long memberId);

    // AI작성: 계주 권한과 중복 신청을 확인한 뒤 삭제 심사를 대기 상태로 등록한다.
    void requestSavingGroupDeletion(Long savingGroupId, Long memberId, String reason);
}
