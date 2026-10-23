package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 회원 목록 한 행에 표시할 마스킹된 회원 정보를 담는다.
public class AdminMemberListItemResponse {
    private Long memberId;
    private String name;
    private String nickname;
    private String email;
    private String regionName;
    private String accountStatus;
    private LocalDateTime identityVerifiedAt;
    private LocalDateTime regionVerifiedAt;
    private LocalDateTime createdAt;
}
