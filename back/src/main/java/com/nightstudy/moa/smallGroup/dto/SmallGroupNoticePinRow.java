package com.nightstudy.moa.smallGroup.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
// AI작성: 잠긴 공지의 고정 상태를 서비스에서 검증한다.
@Getter @Setter @NoArgsConstructor
public class SmallGroupNoticePinRow { private Long noticeId; private Boolean pinned; }
