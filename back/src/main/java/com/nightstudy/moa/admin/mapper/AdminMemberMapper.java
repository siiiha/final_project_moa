package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListItemResponse;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 회원 목록·상세 조회 SQL을 연결한다.
public interface AdminMemberMapper {
    int selectMemberCount(AdminMemberSearchCondition condition);

    List<AdminMemberListItemResponse> selectMemberList(AdminMemberSearchCondition condition);

    AdminMemberDetailResponse selectMemberDetail(@Param("memberId") Long memberId);
}
