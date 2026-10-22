package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionResponse;
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

    // AI작성: 해당 사건에 접수된 신고자별 상세 내용을 조회한다.
    List<AdminReportSubmissionResponse> selectReportSubmissions(
            @Param("caseId") Long caseId
    );

    // AI작성: 중복 처리를 막기 위해 처리 대상 신고 행을 잠금 조회한다.
    String selectReportStatusForUpdate(@Param("reportId") Long reportId);

    // AI작성: 신고 유형에 따라 회원, 게시글 작성자 또는 댓글 작성자를 찾는다.
    Long selectReportedMemberId(@Param("reportId") Long reportId);

    // AI작성: 탈퇴 회원 제한 방지와 기존 제한 연장을 위해 현재 회원 상태를 확인한다.
    String selectMemberAccountStatus(@Param("memberId") Long memberId);

    // AI작성: 기존 제한이 새 제한보다 길거나 영구인지 판단하기 위한 종료 시각이다.
    LocalDateTime selectMemberSuspendedUntil(@Param("memberId") Long memberId);

    // AI작성: PENDING 신고만 최종 상태로 변경하여 중복 처리를 한 번 더 방지한다.
    int updateReportProcess(
            @Param("reportId") Long reportId,
            @Param("status") String status,
            @Param("processReason") String processReason,
            @Param("restrictedUntil") LocalDateTime restrictedUntil
    );

    // AI작성: 계산된 종료 시각과 SUSPENDED 상태를 회원 정보에 함께 반영한다.
    int updateMemberRestriction(
            @Param("memberId") Long memberId,
            @Param("suspendedUntil") LocalDateTime suspendedUntil
    );

    // AI작성: 종료 시각이 지난 기간제 제한 회원을 ACTIVE 상태로 일괄 변경한다.
    int releaseExpiredMemberRestrictions();
}
