package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionResponse;
import com.nightstudy.moa.admin.dto.AdminReportTargetResponse;
import java.util.List;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 신고 조회·처리와 회원 이용 제한에 필요한 SQL을 연결하는 MyBatis Mapper이다.
public interface AdminReportMapper {

    // AI작성: 검색 조건과 페이징이 적용된 신고 사건 목록을 조회한다.
    List<AdminReportListItemResponse> selectReportList(
            AdminReportSearchCondition condition
    );

    // AI작성: 같은 검색 조건에 해당하는 전체 신고 사건 수를 조회한다.
    int selectReportCount(
            AdminReportSearchCondition condition
    );

    // AI작성: 신고 사건 한 건의 기본 정보를 조회한다.
    AdminReportListItemResponse selectReportDetail(
            @Param("caseId") Long caseId
    );

    // AI작성: 신고 사건의 게시글·댓글 원문을 삭제 여부와 관계없이 조회합니다.
    AdminReportTargetResponse selectReportTargetContent(
            @Param("caseId") Long caseId
    );

    // AI작성: 해당 사건에 접수된 신고자별 상세 내용을 조회한다.
    List<AdminReportSubmissionResponse> selectReportSubmissions(
            @Param("caseId") Long caseId
    );

    // AI작성: 중복 처리를 막고 콘텐츠 유형을 확인하기 위해 처리 대상 신고 사건을 잠금 조회한다.
    AdminReportListItemResponse selectReportForUpdate(@Param("reportId") Long reportId);

    // AI작성: 같은 신고 사건을 접수한 모든 신고자의 회원 번호를 중복 없이 조회한다.
    List<Long> selectReportReporterIds(@Param("reportId") Long reportId);

    // AI작성: 신고 유형에 따라 회원, 게시글 작성자 또는 댓글 작성자를 찾는다.
    Long selectReportedMemberId(@Param("reportId") Long reportId);

    // AI작성: 승인 시 관리자가 선택한 신고 게시글을 소프트 삭제한다.
    int deleteReportedPost(@Param("reportId") Long reportId);

    // AI작성: 승인 시 관리자가 선택한 신고 댓글을 소프트 삭제한다.
    int deleteReportedComment(@Param("reportId") Long reportId);

    // AI작성: PENDING 신고만 최종 상태로 변경하여 중복 처리를 한 번 더 방지한다.
    int updateReportProcess(
            @Param("reportId") Long reportId,
            @Param("status") String status,
            @Param("processReason") String processReason,
            @Param("restrictedUntil") LocalDateTime restrictedUntil
    );

}
