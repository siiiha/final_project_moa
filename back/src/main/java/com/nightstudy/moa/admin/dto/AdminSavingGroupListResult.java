package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// AI작성: 관리자 계모임 목록과 페이징 정보를 함께 전달한다.
public class AdminSavingGroupListResult {
    private final List<AdminSavingGroupListItemResponse> savingGroups;
    private final PageMeta pageMeta;
}
