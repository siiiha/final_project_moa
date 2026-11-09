package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI작성: 공지 작성과 동일하게 제목·내용을 모두 검증하고 작성자·고정 상태는 변경하지 않는다.
@Getter
@Setter
public class SavingGroupNoticeUpdateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 5000, message = "내용은 5,000자 이하로 입력해주세요.")
    private String content;
}
