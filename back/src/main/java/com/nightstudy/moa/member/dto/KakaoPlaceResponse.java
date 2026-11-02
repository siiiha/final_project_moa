package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

// AI작성: 카카오 장소 검색에서 필요한 항목만 읽고 외부 응답을 API에 직접 반환하지 않는다.
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoPlaceResponse {
    private Meta meta;
    private List<Place> documents;

    // AI작성: 검색 종료 여부는 외부 API의 실제 메타 정보로 판단한다.
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("total_count")
        private Integer totalCount;
        @JsonProperty("pageable_count")
        private Integer pageableCount;
        @JsonProperty("is_end")
        private Boolean end;
    }

    // AI작성: 좌표와 거리는 외부 원본 문자열로 수신하고 Service에서 검증·변환한다. 전화번호는 읽지 않는다.
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private String id;
        @JsonProperty("place_name")
        private String placeName;
        @JsonProperty("road_address_name")
        private String roadAddress;
        @JsonProperty("address_name")
        private String address;
        @JsonProperty("category_name")
        private String category;
        @JsonProperty("x")
        private String longitude;
        @JsonProperty("y")
        private String latitude;
        @JsonProperty("place_url")
        private String placeUrl;
        private String distance;
    }
}
