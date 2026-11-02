package com.nightstudy.moa.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 제한 처리 중 잠근 계모임의 내부 그룹 번호와 현재 상태를 전달한다.
public class AdminSavingGroupRestrictionTarget {
    private Long savingGroupId;
    private Long groupId;
    private String status;
}
