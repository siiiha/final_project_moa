package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminSmallGroupRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminSmallGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSmallGroupSearchCondition;
import com.nightstudy.moa.admin.dto.AdminSmallGroupStatus;
import com.nightstudy.moa.admin.dto.RestrictionAction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 소모임 운영정지·해제와 제한 이력 저장 SQL의 계약을 정의한다.
public interface AdminSmallGroupMapper {
    int selectSmallGroupCount(AdminSmallGroupSearchCondition condition);

    List<AdminSmallGroupListItemResponse> selectSmallGroupList(AdminSmallGroupSearchCondition condition);

    AdminSmallGroupRestrictionTarget selectRestrictionTargetForUpdate(
            @Param("smallGroupId") Long smallGroupId
    );

    String selectLatestRestrictionAction(@Param("smallGroupId") Long smallGroupId);

    int updateSmallGroupStatus(
            @Param("smallGroupId") Long smallGroupId,
            @Param("currentStatus") AdminSmallGroupStatus currentStatus,
            @Param("nextStatus") AdminSmallGroupStatus nextStatus
    );

    int insertRestrictionHistory(
            @Param("smallGroupId") Long smallGroupId,
            @Param("action") RestrictionAction action,
            @Param("reason") String reason
    );
}
