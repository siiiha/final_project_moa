package com.nightstudy.moa.savingGroup.dto;

// AI작성: 채팅방 번호와 현재 회원의 채팅 이용 제한 여부를 전달한다.
public record SavingGroupChatRoomResponse(
        long roomId,
        boolean chatRestricted
) {
}