package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 목록 조회는 최대 세 번의 SELECT로 처리한다.
@Mapper
public interface SavingGroupMapper {
    long selectSavingGroupCount(SavingGroupListCondition condition);
    List<SavingGroupRow> selectSavingGroupList(SavingGroupListCondition condition);
    List<SavingGroupMemberCount> selectApprovedMemberCounts(@Param("groupIds") List<Long> groupIds);
}
