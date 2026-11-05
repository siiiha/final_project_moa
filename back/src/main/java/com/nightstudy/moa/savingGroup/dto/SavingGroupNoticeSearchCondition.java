package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 기존 계모임 상세 API에서 공지 페이지와 정렬만 바인딩하며 페이지 크기는 서버에서 4개로 고정한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupNoticeSearchCondition {
    @NotNull(message = "공지 페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "공지 페이지 번호는 1 이상이어야 합니다.")
    private Integer noticePage = 1;

    @NotNull(message = "공지 정렬을 선택해 주세요.")
    @Pattern(regexp = "LATEST|OLDEST", message = "공지 정렬은 최신순 또는 과거순으로 선택해 주세요.")
    private String noticeSort = "LATEST";
}
