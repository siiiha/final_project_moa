package com.nightstudy.moa.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 로그인 회원이 신고할 대상과 사유를 전달한다.
public class ReportCreateRequest {
    @NotNull(message = "신고 유형을 선택해 주세요.")
    private ReportType reportType;

    @NotNull(message = "신고 대상을 확인해 주세요.")
    @Positive(message = "신고 대상 번호는 1 이상이어야 합니다.")
    private Long targetId;

    @NotBlank(message = "신고 사유를 입력해 주세요.")
    @Size(max = 500, message = "신고 사유는 500자 이내로 입력해 주세요.")
    private String reason;

    public void setReason(String reason) {
        this.reason = reason == null ? null : reason.strip();
    }
}
