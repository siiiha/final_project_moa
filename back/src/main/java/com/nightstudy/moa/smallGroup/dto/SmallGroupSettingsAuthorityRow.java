package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: MyBatis가 설정 조회·수정 권한 검증에 필요한 소모임, 개설자, 승인 OWNER 식별자를 담는다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupSettingsAuthorityRow {
    private Long smallGroupId;
    private Long groupId;
    private Long groupCreatorMemberId;
    private Long ownerMemberId;
}
