package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberTermDto {

    private Long termId;
    private String title;
    private String version;
    private String content;
    private Boolean required;
}
