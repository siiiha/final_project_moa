package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import java.util.List;

// AI작성: 소모임 공개 목록의 검색·필터 조회 계약이다.
public interface SmallGroupService {
    List<SmallGroupSummaryResponse> getSmallGroupList(SmallGroupSearchCondition condition);
}
