package com.nightstudy.moa.post.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostSearchCondition {
    // AI작성: DB 카테고리 기본 키 타입과 맞춰 숫자로 받는다.
    private Long categoryId;

    private String keyword;
    private String searchType;

    //사용자가 보고자하는 페이지 (기본1)
    private int page = 1;
    //페이징
    private int size = 10;

    // AI작성: 허용되지 않은 값은 Mapper에서 기본 최신순(desc)으로 처리한다.
    private String order = "desc";

    // service에서 Page정보로 직접 계산을 해서 사용하는 값
    // offset = (page - 1) * size;
    private int offset;
    private int limit;
}
