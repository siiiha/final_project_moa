package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminDashboardSummaryResponse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// AI작성: 관리자 대시보드의 회원 현황 집계 SQL을 연결한다.
public interface AdminDashboardMapper {
    AdminDashboardSummaryResponse selectDashboardSummary();
}
