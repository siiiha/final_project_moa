package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 투표 참여·선택 변경 요청의 보기 식별자만 받고 실제 소속·중복·선택 수 검증은 Service가 수행한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteParticipationRequest {
    @NotNull(message = "선택한 투표 보기를 입력해 주세요.")
    @NotEmpty(message = "투표 보기를 한 개 이상 선택해 주세요.")
    private List<Long> selectedOptionIds;
}
