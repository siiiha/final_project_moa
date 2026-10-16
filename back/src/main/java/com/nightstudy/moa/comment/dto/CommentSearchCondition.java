package com.nightstudy.moa.comment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentSearchCondition {

    private Long postId;
    // 요청 페이지 번호
    private int page = 1;
    // 페이지당 조회 개수
    private int size = 20;
    // Service에서 계산한 DB 조회 시작 위치
    private int offset;
    // Service에서 계산한 DB 조회 개수
    private int limit;
}
