package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 목록 조회는 최대 세 번의 SELECT로 처리한다.
@Mapper
public interface SavingGroupMapper {
    // 목록 검색 조건에 맞는 공개 계모임의 총 개수를 조회한다.
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
}
