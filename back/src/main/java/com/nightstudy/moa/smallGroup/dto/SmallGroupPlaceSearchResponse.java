package com.nightstudy.moa.smallGroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공통 ApiResponse의 data에 담을 장소 검색 결과이며 검색 기준인 사용자 좌표는 반환하지 않는다.
@Getter
@AllArgsConstructor
public class SmallGroupPlaceSearchResponse {
    private final List<Place> items;
    private final int page;
    private final int size;
    private final int totalCount;
    private final int pageableCount;
    private final boolean end;

    // AI작성: 장소 좌표만 반환하며 distanceMeters는 기준 좌표가 없는 검색에서는 null이다.
    @Getter
    @AllArgsConstructor
    public static class Place {
        private final String placeId;
        private final String placeName;
        private final String roadAddress;
        private final String address;
        private final String category;
        private final Double latitude;
        private final Double longitude;
        private final String placeUrl;
        private final Integer distanceMeters;
    }
}
