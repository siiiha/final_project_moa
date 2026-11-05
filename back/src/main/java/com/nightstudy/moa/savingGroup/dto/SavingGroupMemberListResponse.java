package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 계원 배열과 최대 두 페이지의 메타데이터를 기존 응답 형식으로 전달한다.
@Getter
@AllArgsConstructor
public class SavingGroupMemberListResponse {
    private final List<SavingGroupMemberResponse> items;
    private final PageMeta pageMeta;
}
