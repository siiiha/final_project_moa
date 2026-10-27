package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;

// AI작성: 소모임 공개 목록의 검색·필터 조회 계약이다.
public interface SmallGroupService {
    SmallGroupListResponse getSmallGroupList(SmallGroupSearchCondition condition);
}
