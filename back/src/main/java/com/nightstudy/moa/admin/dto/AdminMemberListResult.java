package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// AI작성: 관리자 회원 목록과 페이징 정보를 함께 전달한다.
public class AdminMemberListResult {
    private final List<AdminMemberListItemResponse> members;
    private final PageMeta pageMeta;
}
