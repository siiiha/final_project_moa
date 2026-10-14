package com.nightstudy.moa.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PostEditRequest {
    @NotNull(message = "카테고리는 필수입니다.")
    @Positive(message = "카테고리 값이 올바르지 않습니다.")
    private Long categoryId;
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
}
