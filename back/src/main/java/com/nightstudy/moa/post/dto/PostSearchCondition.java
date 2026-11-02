package com.nightstudy.moa.post.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostSearchCondition {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_PAGE = 10_000;
    private static final int MAX_SIZE = 100;

    private Long categoryId;
    private String keyword;
    private String searchType;

    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;

    private String order = "desc";

    private int offset;
    private int limit;

    // AI작성: 과도한 페이지 번호와 offset 정수 오버플로를 방지한다.
    public void setPage(int page) {
        this.page = page > 0
                ? Math.min(page, MAX_PAGE)
                : DEFAULT_PAGE;
    }

    // AI작성: 한 번에 지나치게 많은 게시글을 조회하지 않도록 크기를 제한한다.
    public void setSize(int size) {
        this.size = size > 0
                ? Math.min(size, MAX_SIZE)
                : DEFAULT_SIZE;
    }
}
