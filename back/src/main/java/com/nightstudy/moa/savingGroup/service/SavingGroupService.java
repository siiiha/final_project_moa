package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinApprovalTargetRow;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    // AI작성: 승인 대상 검증과 기존 신청 행의 상태 변경을 하나의 트랜잭션에서 처리한다.
    void approveSavingGroupJoin(Long savingGroupId, Long crewMemberId, Long memberId);

    // AI작성: 호출자의 기존 트랜잭션 안에서만 검증하며, 반환값을 별도 트랜잭션의 승인 근거로 사용하지 않는다.
    SavingGroupJoinApprovalTargetRow validateSavingGroupJoinApproval(
            Long savingGroupId, Long crewMemberId, Long memberId);

    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);

    // AI작성: 인증된 회원 본인 명의로 계모임 가입 신청을 대기 상태로 등록한다.
    void requestSavingGroupJoin(Long savingGroupId, Long memberId);
}
