package com.nightstudy.moa.smallGroup.service;

import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import java.util.List;

// AI작성: 소모임 생성과 개설 화면의 활동 지역 조회 규칙을 Controller와 분리한 Service 계약이다.
public interface SmallGroupCreationService {
    List<SmallGroupRegionResponse> getSmallGroupRegions();

    SmallGroupCreationResponse createSmallGroup(SmallGroupCreationRequest request);
}
