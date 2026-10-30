package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 회원 상세에 현재 적용 중인 기능별 제한 범위와 종료 시각을 전달한다.
public class AdminMemberFeatureRestrictionResponse {
    private MemberRestrictionScope restrictionScope;
    private LocalDateTime restrictedUntil;
}
