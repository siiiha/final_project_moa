package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공지 저장 전용 데이터이며 내부 모임 ID와 인증된 작성자 ID는 Service에서 설정한다.
@Getter
@AllArgsConstructor
public class SavingGroupNoticeCreateRow {
    private final Long groupId;
    private final Long writerId;
    private final String title;
    private final String content;
}
