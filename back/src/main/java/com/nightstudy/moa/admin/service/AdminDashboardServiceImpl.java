package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminDashboardSummaryResponse;
import com.nightstudy.moa.admin.mapper.AdminDashboardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 대시보드 집계 조회를 읽기 전용 트랜잭션으로 처리한다.
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final AdminDashboardMapper adminDashboardMapper;

    public AdminDashboardServiceImpl(AdminDashboardMapper adminDashboardMapper) {
        this.adminDashboardMapper = adminDashboardMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getDashboardSummary() {
        return adminDashboardMapper.selectDashboardSummary();
    }
}
