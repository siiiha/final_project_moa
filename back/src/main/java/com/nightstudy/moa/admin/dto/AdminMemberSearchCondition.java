package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 회원 목록의 검색·상태·지역·페이징 조건을 받는다.
public class AdminMemberSearchCondition {
    @Size(max = 40, message = "검색어는 40자 이내로 입력해 주세요.")
    private String keyword;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "회원 상태를 다시 선택해 주세요.")
    private String accountStatus;

    @Size(max = 100, message = "지역명은 100자 이내로 입력해 주세요.")
    private String regionName;

    // AI작성: 현재 적용 중인 기능 제한 영역으로 관리자 회원 목록을 필터링합니다.
    private MemberRestrictionScope restrictionScope;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    private int offset;
    private int limit;

    public void setKeyword(String keyword) {
        this.keyword = keyword == null || keyword.strip().isEmpty() ? null : keyword.strip();
    }
}
