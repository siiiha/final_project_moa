package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자의 신고 처리 결과, 기능별 제한 범위·기간과 판단 사유를 받는 요청 DTO이다.
public class AdminReportProcessRequest {

    // AI작성: 관리자가 선택할 수 있는 최종 결과를 DB 상태값과 동일하게 제한한다.
    @NotBlank(message = "처리 결과는 필수입니다.")
    @Pattern(
            regexp = "CONFIRMED|REJECTED",
            message = "처리 결과는 CONFIRMED 또는 REJECTED만 가능합니다."
    )
    private String status;

    // AI작성: CONFIRMED 처리일 때 제한할 CONTENT, CHAT, REPORT 기능 범위를 받는다.
    private MemberRestrictionScope restrictionScope;

    // AI작성: CONFIRMED 처리일 때 적용할 회원 이용 제한 기간이다.
    @Pattern(
            regexp = "7_DAYS|30_DAYS|PERMANENT",
            message = "이용 제한은 7_DAYS, 30_DAYS, PERMANENT 중 하나여야 합니다."
    )
    private String restrictionType;

    // AI작성: 기존 reason은 사용자 신고 내용이므로 관리자 판단 사유를 별도로 받는다.
    @NotBlank(message = "처리 사유는 필수입니다.")
    @Size(max = 500, message = "처리 사유는 500자 이하여야 합니다.")
    private String processReason;
}
