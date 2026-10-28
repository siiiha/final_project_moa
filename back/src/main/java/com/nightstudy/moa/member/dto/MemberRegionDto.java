package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRegionDto {

    private Long regionId;
    private String regionName;
    private String provinceName;
    private String districtName;
}
