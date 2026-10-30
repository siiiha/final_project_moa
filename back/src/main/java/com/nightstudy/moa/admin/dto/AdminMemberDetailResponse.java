package com.nightstudy.moa.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 회원 상세 화면에 필요한 마스킹 정보와 활동 집계를 담는다.
public class AdminMemberDetailResponse {
    private Long memberId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String gender;
    private String regionName;
    private String userLevel;
    private String systemRole;
    private String accountStatus;
    private List<AdminMemberFeatureRestrictionResponse> restrictions;
    private LocalDateTime identityVerifiedAt;
    private LocalDateTime regionVerifiedAt;
    private LocalDateTime createdAt;
    private Integer joinedGroupCount;
    private Integer writtenPostCount;
}
