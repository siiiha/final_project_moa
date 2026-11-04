package com.nightstudy.moa.smallGroup.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
// AI작성: 고정 여부는 명시적인 true·false만 허용한다.
@Getter @Setter
public class SmallGroupNoticePinRequest {
    @NotNull(message = "고정 여부를 선택해 주세요.") private Boolean pinned;
}
