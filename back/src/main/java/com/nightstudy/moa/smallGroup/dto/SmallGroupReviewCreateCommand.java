package com.nightstudy.moa.smallGroup.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: Service가 검증한 후기 저장값과 MyBatis가 반환할 생성 키를 전달하는 내부 객체다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupReviewCreateCommand {
    private Long smallGroupReviewId;
    private Long smallGroupId;
    private Long writerId;
    private BigDecimal rating;
    private String content;
}
