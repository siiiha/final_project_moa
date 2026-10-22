package com.nightstudy.moa.comment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentSearchCondition {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 20;
    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_PAGE = 10_000;

    private Long postId;
    // 요청 페이지 번호
    private int page = DEFAULT_PAGE;
    // 페이지당 조회 개수
    private int size = DEFAULT_SIZE;
    // Service에서 계산한 DB 조회 시작 위치
    private int offset;
    // Service에서 계산한 DB 조회 개수
    private int limit;

    // AI작성: 댓글은 한 페이지에 최대 20개까지만 조회하도록 요청값을 제한합니다.
    public void setSize(int size) {
        this.size = size > 0 ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
    }

    // AI작성: 지나치게 큰 페이지 번호로 offset 정수 오버플로가 발생하지 않도록 제한합니다.
    public void setPage(int page) {
        this.page = page > 0 ? Math.min(page, MAX_PAGE) : DEFAULT_PAGE;
    }
}
