package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 목록 조회는 최대 세 번의 SELECT로 처리한다.
@Mapper
public interface SavingGroupMapper {
    int selectSavingGroupCount(SavingGroupListCondition condition);
    List<SavingGroupRow> selectSavingGroupList(SavingGroupListCondition condition);
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
}
