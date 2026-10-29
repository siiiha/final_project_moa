package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 카카오 지역 조회 응답에서 비교에 필요한 정보만 읽음
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoRegionResponse {

    private List<Region> documents;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Region {

        @JsonProperty("region_type")
        private String regionType;

        @JsonProperty("region_1depth_name")
        private String provinceName;

        @JsonProperty("region_2depth_name")
        private String districtName;
    }
}