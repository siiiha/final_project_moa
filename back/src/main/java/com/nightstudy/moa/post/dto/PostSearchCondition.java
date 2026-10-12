package com.nightstudy.moa.post.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostSearchCondition {
    private String categoryId;

    private String keyword;
    private String searchType;

    //사용자가 보고자하는 페이지 (기본1)
    private int page = 1;
    //페이징
    private int size = 10;

    // service에서 Page정보로 직접 계산을 해서 사용하는 값
    // offset = (page - 1) * size;
    private int offset;
    private int limit;
}
