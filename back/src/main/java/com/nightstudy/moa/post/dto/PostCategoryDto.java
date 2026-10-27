package com.nightstudy.moa.post.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 게시글 목록·작성 화면에서 사용하는 활성 카테고리 정보다.
@Getter
@Setter
public class PostCategoryDto {
    private Long categoryId;
    private String categoryName;
}
