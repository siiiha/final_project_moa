package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 소모임 투표 상세 화면에 필요한 공개 정보와 로그인 회원의 참여 정보를 함께 반환한다.
@Getter
@AllArgsConstructor
public class SmallGroupVoteDetailResponse {
    private final Long smallGroupVoteId;
    private final String title;
    private final String creatorNickname;
    private final boolean multipleChoice;
    private final boolean anonymous;
    private final LocalDateTime createdAt;
    private final LocalDateTime endsAt;
    private final String status;
    private final long participantCount;
    private final SmallGroupVoteMyParticipationResponse myParticipation;
    private final List<SmallGroupVoteOptionDetailResponse> options;
}
