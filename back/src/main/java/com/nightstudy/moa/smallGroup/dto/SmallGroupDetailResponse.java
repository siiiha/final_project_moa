package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공개 상세 화면에 필요한 소모임 정보와 로그인 사용자의 역할만 반환한다.
@Getter
@AllArgsConstructor
public class SmallGroupDetailResponse {
    private final Long smallGroupId;
    private final Long groupId;
    private final String groupName;
    private final String description;
    private final String regionName;
    private final String likeCategory;
    private final LocalDate meetDate;
    private final String location;
    private final Integer maxMembers;
    private final long memberCount;
    private final String creatorNickname;
    private final LocalDateTime createdAt;
    // AI작성: 비로그인 또는 비회원 공개 조회에서는 null이며, 승인 OWNER/MEMBER만 역할을 가진다.
    private final String myRole;
    // AI작성: 공개 공지 목록 UI에 표시할 최신 4건의 제목·작성자·작성일 요약이다.
    private final List<SmallGroupNoticeSummaryResponse> notices;
}
