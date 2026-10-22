package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 목록 조회는 최대 세 번의 SELECT로 처리한다.
@Mapper
public interface SavingGroupMapper {
    // 목록 검색 조건에 맞는 공개 계모임의 총 개수를 조회한다.
    // AI작성: 일대다 데이터를 분리 조회하여 공지 수가 계원 집계에 영향을 주지 않게 한다.
    SavingGroupRow selectSavingGroupDetail(@Param("groupId") Long groupId);
    long selectDetailMemberCount(@Param("groupId") Long groupId);
    List<SavingGroupNoticeSummaryResponse> selectRecentNotices(@Param("groupId") Long groupId);
    List<String> selectApprovedMemberRoles(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
    int selectSavingGroupCount(SavingGroupListCondition condition);

    // 목록 검색 조건에 맞는 공개 계모임의 기본 정보를 조회한다.
    List<SavingGroupRow> selectSavingGroupList(SavingGroupListCondition condition);

    // 전달받은 모임 ID별 승인 계원 수를 조회한다.
    List<SavingGroupMemberCount> selectApprovedMemberCounts(@Param("groupIds") List<Long> groupIds);

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

    // 승인 계원 행을 잠근 뒤 정원 검증에 사용할 계원 수를 집계한다.
    long selectApprovedMemberCountForUpdate(@Param("groupId") Long groupId);

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
    // AI작성: 가입 신청 전 회원의 계정 상태와 본인 인증 완료 여부를 잠근 상태로 조회한다.
    SavingGroupJoinMemberRow selectSavingGroupJoinMemberForUpdate(@Param("memberId") Long memberId);

    // AI작성: 가입 신청 대상의 내부 모임 ID, 상태, 정원을 잠근 상태로 조회한다.
    SavingGroupJoinTargetRow selectSavingGroupJoinTargetForUpdate(@Param("savingGroupId") Long savingGroupId);

    // AI작성: 동일 회원의 기존 가입·신청 이력을 잠근 상태로 조회한다.
    SavingGroupMembershipRelationRow selectSavingGroupMembershipForUpdate(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 승인된 실제 가입만 집계하고 종료·삭제 모임은 최대 가입 수에서 제외한다.
    long selectApprovedSavingGroupCount(@Param("memberId") Long memberId);

    // AI작성: 추가 입력 없이 인증 회원 본인의 가입 신청을 대기 상태로 저장한다.
    int insertSavingGroupJoinRequest(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 자진 탈퇴한 회원만 기존 관계 행을 대기 신청 상태로 되돌린다.
    int reactivateLeftSavingGroupMembership(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);
}
