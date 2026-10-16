package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportDetailResponse;
import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionResponse;
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

        if(total == 0) {
            return new AdminReportListResult(
                    List.of(),
                    pageMeta
            );
        }

        //계산된 페이징 값을 검색 조건 객체에 저장
        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        List<AdminReportListItemResponse> reports =
                adminReportMapper.selectReportList(condition);
        return new AdminReportListResult(reports, pageMeta);
    }

    @Override
    public AdminReportDetailResponse getReportDetail(Long caseId) {
        AdminReportListItemResponse reportCase =
                adminReportMapper.selectReportDetail(caseId);

        if (reportCase == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "REPORT_NOT_FOUND",
                    "신고 사건을 찾을 수 없습니다."
            );
        }

        List<AdminReportSubmissionResponse> submissions =
                adminReportMapper.selectReportSubmissions(caseId);

        AdminReportDetailResponse result = new AdminReportDetailResponse(reportCase, submissions);
        return result;
    }
}
