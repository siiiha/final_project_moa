package com.nightstudy.moa.admin.dto;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
// AI작성: 관리자 신고 목록의 검색·필터·페이징 조건을 받는 DTO이다.
public class AdminReportSearchCondition {

    // AI작성: 신고 처리 상태 필터이다.
    // PENDING, CONFIRMED, REJECTED
    private String status;

    // AI작성: 신고 유형 필터이다.
    // BOARD_POST, BOARD_COMMENT, BOARD_MEMBER,
    // MEETING, MEETING_MEMBER
    private String reportType;

    // AI작성: 검색어를 적용할 대상 필드이다.
    // reportedMember, reporter, processReason
    private String searchType;

    // AI작성: 회원 또는 처리 사유를 찾을 검색어이다.
    private String keyword;

    // AI작성: 사용자가 요청한 페이지 번호이며 기본값은 1이다.
    private int page = 1;

    // AI작성: 한 페이지에 표시할 개수이며 기본값은 10이다.
    private int size = 10;

    // AI작성: Service가 page와 size로 계산해 Mapper의 LIMIT 절에 전달하는 값이다.
    private int offset;
    private int limit;


}

