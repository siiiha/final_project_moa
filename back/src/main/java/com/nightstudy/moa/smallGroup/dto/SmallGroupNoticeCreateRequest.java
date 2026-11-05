package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI작성: 기존 게시글과 동일한 제목·내용 검증을 적용하며 작성자는 요청으로 받지 않는다.
@Getter
@Setter
public class SmallGroupNoticeCreateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 5000, message = "내용은 5,000자 이하로 입력해주세요.")
    private String content;
}
