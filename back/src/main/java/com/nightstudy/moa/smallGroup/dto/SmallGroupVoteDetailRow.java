package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: MyBatis 투표 상세 원본 조회 결과를 Service의 권한·마감 정책 조합에만 사용한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteDetailRow {
    private Long smallGroupVoteId;
    private Long smallGroupId;
    private Long creatorMemberId;
    private String creatorNickname;
    private String title;
    private boolean multipleChoice;
    private boolean anonymous;
    private LocalDateTime endsAt;
    private String status;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
