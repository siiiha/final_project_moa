package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 계모임 목록·상세 조회 SQL을 연결한다.
public interface AdminSavingGroupMapper {
    int selectSavingGroupCount(AdminSavingGroupSearchCondition condition);

    List<AdminSavingGroupListItemResponse> selectSavingGroupList(
            AdminSavingGroupSearchCondition condition);

    AdminSavingGroupDetailResponse selectSavingGroupDetail(
            @Param("savingGroupId") Long savingGroupId
    );
}
