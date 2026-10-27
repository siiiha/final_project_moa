package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import java.util.List;

// AI작성: 소모임 공개 목록 조회 계약이다.
public interface SmallGroupService {
    List<SmallGroupSummaryResponse> getSmallGroupList();
}
