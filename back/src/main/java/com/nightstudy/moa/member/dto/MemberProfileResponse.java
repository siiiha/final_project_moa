package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MemberProfileResponse {

    private Long memberId;
    private String name;
    private String email;
    private String phone;
    private String nickname;
    private Integer age;
    private String gender;
    private String profileImageUrl;
    private Long regionId;
    private String regionName;
    private String provinceName;
    private String districtName;
    private boolean identityVerified;
    private boolean regionVerified;

    private List<Interest> interests = new ArrayList<>();

    @Getter
    @Setter
    public static class Interest {

        private Long interestId;
        private String interestName;
    }
}
