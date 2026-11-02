package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 관리자 개설 심사 목록의 검색어·상태·페이지를 검증한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupCreationApplicationSearchCondition {
    @Size(max = 20, message = "검색어는 20자 이내로 입력해 주세요.")
    private String keyword;

    @Pattern(regexp = "PENDING|APPROVED|REJECTED", message = "심사 상태를 다시 선택해 주세요.")
    private String status;

    @Pattern(regexp = "CREATE|DELETE", message = "심사 유형을 다시 선택해 주세요.")
    private String type;

    @NotNull(message = "페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    public void setKeyword(String keyword) {
        this.keyword = keyword == null || keyword.strip().isEmpty() ? null : keyword.strip();
    }
}
