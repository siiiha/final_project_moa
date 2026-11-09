package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 계주 전용 응답으로 이름 원문·마스킹 이메일·계모임 가입일만 제공한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupMemberResponse {
    private String memberId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime joinedAt;
}
