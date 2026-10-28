package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;

// AI작성: 소모임 공개 목록의 검색·필터 조회 계약이다.
public interface SmallGroupService {
    SmallGroupListResponse getSmallGroupList(SmallGroupSearchCondition condition);

    // AI작성: memberId가 null인 비로그인 공개 조회도 허용하는 상세 조회 계약이다.
    SmallGroupDetailResponse getSmallGroupDetail(Long smallGroupId, Long memberId);
}
