package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportDetailResult;
import com.nightstudy.moa.admin.dto.AdminReportListItem;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionItem;
import com.nightstudy.moa.admin.mapper.AdminReportMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;



@Service
public class AdminReportServiceImpl implements AdminReportService {
    private final AdminReportMapper adminReportMapper;

    @Autowired
    public AdminReportServiceImpl(AdminReportMapper adminReportMapper){
        this.adminReportMapper = adminReportMapper;
    }

    @Override
    public AdminReportListResult getReportList (AdminReportSearchCondition condition){
        int total = adminReportMapper.selectReportCount(condition);
        PageMeta pageMeta = PageMeta.of(
                condition.getPage(),
                condition.getSize(),
                total
        );

        List<AdminReportListItem> reports =
                adminReportMapper.selectReportList(
                        condition,
                        pageMeta.getOffset(),
                        pageMeta.getSize()
                );
        return new AdminReportListResult(reports, pageMeta);
    }

    @Override
    public AdminReportDetailResult getReportDetail(Long caseId) {
        AdminReportListItem reportCase = adminReportMapper.selectReportDetail(caseId);

        if (reportCase == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORT_NOT_FOUND",
                    "신고글을 찾을 수 없습니다."
            );
        }

        List<AdminReportSubmissionItem> submissions =
                adminReportMapper.selectReportSubmissions(caseId);

        return new AdminReportDetailResult(reportCase, submissions);
    }
}
