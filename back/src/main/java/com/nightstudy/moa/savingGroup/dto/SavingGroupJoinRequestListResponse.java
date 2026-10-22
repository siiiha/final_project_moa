package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 가입 신청 목록도 기존 data 배열·meta 페이지 응답 구조를 사용한다.
@Getter
@AllArgsConstructor
public class SavingGroupJoinRequestListResponse {
    private final List<SavingGroupJoinRequestRow> items;
    private final PageMeta pageMeta;
}
