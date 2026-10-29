package com.nightstudy.moa.smallGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 가입 신청 목록도 공통 ApiResponse의 data 배열과 meta 페이징 정보를 조합하기 위한 내부 응답이다.
@Getter
@AllArgsConstructor
public class SmallGroupJoinRequestListResponse {
    private final List<SmallGroupJoinRequestRow> items;
    private final PageMeta pageMeta;
}
