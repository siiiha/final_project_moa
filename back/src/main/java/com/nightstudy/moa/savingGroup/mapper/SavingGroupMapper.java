package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 목록 조회는 최대 세 번의 SELECT로 처리한다.
@Mapper
public interface SavingGroupMapper {
    // AI작성: 일대다 데이터를 분리 조회하여 공지 수가 계원 집계에 영향을 주지 않게 한다.
    SavingGroupRow selectSavingGroupDetail(@Param("groupId") Long groupId);
    long selectDetailMemberCount(@Param("groupId") Long groupId);
    List<SavingGroupNoticeSummaryResponse> selectRecentNotices(@Param("groupId") Long groupId);
    List<String> selectApprovedMemberRoles(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
    long selectSavingGroupCount(SavingGroupListCondition condition);
    List<SavingGroupRow> selectSavingGroupList(SavingGroupListCondition condition);
    List<SavingGroupMemberCount> selectApprovedMemberCounts(@Param("groupIds") List<Long> groupIds);
}
