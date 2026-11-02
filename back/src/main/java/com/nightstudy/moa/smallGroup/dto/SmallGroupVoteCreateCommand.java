package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: Service가 권한·입력값을 검증하고 정리한 뒤 MyBatis 저장에 전달하는 내부 명령 객체다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmallGroupVoteCreateCommand {
    private Long smallGroupVoteId;
    private Long smallGroupId;
    private Long creatorMemberId;
    private String title;
    private boolean multipleChoice;
    private boolean anonymous;
    private LocalDateTime endsAt;
    private List<String> options;
}
