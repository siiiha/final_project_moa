package com.nightstudy.moa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 목록 응답의 페이징 정보. 페이지 번호는 1부터 시작한다.
 */
@Getter
@AllArgsConstructor
public class PageMeta {

    private final int page;
    private final int size;
    private final int total;
    private final int totalPages;

    public static PageMeta of(int page, int size, int total) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = size > 0 ? size : 10;
        int normalizedTotal = Math.max(total, 0);
        int totalPages = (normalizedTotal + normalizedSize - 1) / normalizedSize; // 올림 나눗셈
        return new PageMeta(normalizedPage, normalizedSize, normalizedTotal, totalPages);
    }

    public int getOffset() {
        return (page - 1) * size;
    }
}