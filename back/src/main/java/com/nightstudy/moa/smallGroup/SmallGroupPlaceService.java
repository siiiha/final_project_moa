package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchResponse;

// AI작성: 승인된 OWNER·MEMBER 권한 확인 후 기존 카카오 Client로 장소를 검색하는 Service 계약이다.
public interface SmallGroupPlaceService {
    SmallGroupPlaceSearchResponse searchPlaces(Long memberId, SmallGroupPlaceSearchRequest request);

    // AI작성: TODO 길찾기는 기존 Local API에서 지원하지 않으므로 별도 요청과 정책 확정 후 구현한다.
}
