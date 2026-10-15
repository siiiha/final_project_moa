package com.nightstudy.moa.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//신고 목록의 필터와 페이지 정보를 받는 DTO
public class AdminReportSearchCondition {

    // 신고 처리 상태
    // PENDING, CONFIRMED, REJECTED
    private String status;

    // 신고 유형
    // BOARD_POST, BOARD_COMMENT, BOARD_MEMBER,
    // MEETING, MEETING_MEMBER
    private String reportType;

    // 검색 대상
    // reportedMember, reporter, processReason
    private String searchType;

    // 검색어
    private String keyword;

    // 요청 페이지 번호
    private int page = 1;

    // 페이지당 조회 개수
    private int size = 10;
}

