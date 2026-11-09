package com.nightstudy.moa.smallGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 공개 목록에는 후기 식별자와 표시 정보만 반환하고 작성자 회원 ID는 노출하지 않는다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupReviewListItemResponse {
    private Long smallGroupReviewId;
    private String writerNickname;
    private String content;
    private LocalDateTime createdAt;
    private BigDecimal rating;
}
