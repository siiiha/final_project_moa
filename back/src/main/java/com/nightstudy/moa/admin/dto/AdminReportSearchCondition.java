package com.nightstudy.moa.admin.dto;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@NoArgsConstructor
@Getter
@Setter
// AI작성: 관리자 신고 목록의 검색·필터·페이징 조건을 받는 DTO이다.
public class AdminReportSearchCondition {

    // AI작성: DB에서 사용하는 신고 처리 상태만 검색 조건으로 허용한다.
    @Pattern(
            regexp = "PENDING|CONFIRMED|REJECTED",
            message = "신고 처리 상태를 다시 선택해 주세요."
    )
    private String status;

    // AI작성: 지원하는 신고 대상 유형만 검색 조건으로 허용한다.
    @Pattern(
            regexp = "BOARD_POST|BOARD_COMMENT|BOARD_MEMBER|MEETING|MEETING_MEMBER",
            message = "신고 유형을 다시 선택해 주세요."
    )
    private String reportType;

    // AI작성: Mapper에서 지원하는 검색 대상만 허용한다.
    @Pattern(
            regexp = "reportedMember|reporter|processReason",
            message = "검색 유형을 다시 선택해 주세요."
    )
    private String searchType;

    @Size(max = 100, message = "검색어는 100자 이내로 입력해 주세요.")
    private String keyword;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    // AI작성: Service가 page와 size로 계산해 Mapper의 LIMIT 절에 전달하는 값이다.
    private int offset;
    private int limit;
}

