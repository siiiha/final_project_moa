package com.nightstudy.moa.smallGroup.service;

import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;

// AI작성: 소모임 생성 비즈니스 규칙을 Controller와 분리한 Service 계약이다.
public interface SmallGroupCreationService {
    SmallGroupCreationResponse createSmallGroup(SmallGroupCreationRequest request);
}
