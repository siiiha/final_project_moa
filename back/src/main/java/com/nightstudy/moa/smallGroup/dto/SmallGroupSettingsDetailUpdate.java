package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: small_group 테이블의 소모임 전용 설정 필드만 MyBatis 동적 UPDATE에 전달한다.
@Getter
@AllArgsConstructor
public class SmallGroupSettingsDetailUpdate {
    private final Long smallGroupId;
    private final String likeCategory;
    private final LocalDate meetDate;
    private final String location;
}
