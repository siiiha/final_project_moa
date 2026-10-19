package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminReportMapper {

    // 검색 조건에 맞는 신고 사건 목록 조회
    List<AdminReportListItemResponse> selectReportList(
            AdminReportSearchCondition condition
    );

    // 검색 조건에 맞는 신고 사건의 전체 개수 조회
    int selectReportCount(
            AdminReportSearchCondition condition
    );

    // 신고 사건 한 건의 기본 정보 조회
    AdminReportListItemResponse selectReportDetail(
            @Param("caseId") Long caseId
    );

    // 해당 사건에 접수된 신고자별 신고 내용 조회
    List<AdminReportSubmissionResponse> selectReportSubmissions(
            @Param("caseId") Long caseId
    );
}
