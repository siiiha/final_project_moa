package com.nightstudy.moa.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PostEditRequest {
    @NotNull(message = "카테고리는 필수입니다.")
    @Positive(message = "카테고리 값이 올바르지 않습니다.")
    private Long categoryId;
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 5000, message = "내용은 5,000자 이하로 입력해주세요.")
    private String content;
}
