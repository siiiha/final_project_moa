package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI작성: 소모임 투표 생성 요청의 원본 입력값을 받고, 공백·중복 보기 정리는 Service에서 처리한다.
@Getter
@NoArgsConstructor
public class SmallGroupVoteCreateRequest {
    @NotNull(message = "투표 제목을 입력해 주세요.")
    private String title;

    @NotNull(message = "투표 보기를 입력해 주세요.")
    @Size(max = 8, message = "투표 보기는 최대 8개까지 입력할 수 있습니다.")
    private List<String> options;

    @NotNull(message = "복수 선택 여부를 선택해 주세요.")
    private Boolean multipleChoice;

    @NotNull(message = "익명 여부를 선택해 주세요.")
    private Boolean anonymous;

    private LocalDateTime endsAt;
}
