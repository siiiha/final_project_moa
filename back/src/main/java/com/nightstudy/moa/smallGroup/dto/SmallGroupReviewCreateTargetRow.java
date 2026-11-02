package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 후기 등록 대상 소모임의 회원 관계·개설자·모임 날짜 검증에 필요한 조회값이다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupReviewCreateTargetRow {
    private Long smallGroupId;
    private Long groupId;
    private Long creatorMemberId;
    private LocalDate meetDate;
}
