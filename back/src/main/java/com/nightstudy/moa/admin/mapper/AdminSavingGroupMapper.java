package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import com.nightstudy.moa.admin.dto.AdminSavingGroupRestrictionTarget;
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

    // AI작성: 관리자 제한·해제의 상태 판단이 동시 요청에 의해 바뀌지 않도록 계모임을 잠근다.
    AdminSavingGroupRestrictionTarget selectRestrictionTargetForUpdate(
            @Param("savingGroupId") Long savingGroupId
    );

    // AI작성: 가장 최근 계모임 제한 이력의 동작을 조회해 일반 일시정지와 관리자 제한을 구분한다.
    String selectLatestRestrictionAction(@Param("savingGroupId") Long savingGroupId);

    // AI작성: 계모임 제한·해제 사유를 기존 공통 제한 이력에 저장한다.
    int insertRestrictionHistory(
            @Param("savingGroupId") Long savingGroupId,
            @Param("action") String action,
            @Param("reason") String reason
    );
}
