package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import java.util.List;

// AI작성: 실시간 전송과 REST 조회에 동일한 채팅방 권한을 적용한다.
public interface SavingGroupChatService {
    long findRoomId(long savingGroupId, Long memberId);
    SavingGroupChatMessage save(long roomId, Long memberId, String content);
    List<SavingGroupChatMessage> history(long roomId, Long memberId, Long beforeId, int size);
}
