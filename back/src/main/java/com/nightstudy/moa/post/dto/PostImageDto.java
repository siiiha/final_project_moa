package com.nightstudy.moa.post.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 게시글 상세에서 반환하는 이미지 식별자와 조회 주소입니다.
@Getter
@Setter
public class PostImageDto {
    private Long imageId;
    private String imageUrl;
}
