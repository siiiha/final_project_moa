package com.nightstudy.moa.savingGroup.dto;

import java.time.Instant;

// AI작성: 채팅방 번호와 현재 회원의 채팅 이용 제한 여부를 전달한다.
public record SavingGroupChatRoomResponse(
        long roomId,
        boolean chatRestricted,
        // AI작성: 관리자 제재와 도배 제한을 구분하고 서버 시간 기준으로 남은 시간을 계산한다.
        boolean spamRestricted,
        Instant restrictedUntil,
        Instant serverTime
) {
}
