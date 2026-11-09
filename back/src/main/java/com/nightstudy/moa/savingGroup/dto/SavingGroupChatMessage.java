package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

// AI작성: 메시지 저장 키와 조회 응답에 같은 필드를 사용한다.
@Getter
@Setter
public class SavingGroupChatMessage {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
}
