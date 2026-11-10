package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupChatMapper;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavingGroupChatServiceImpl implements SavingGroupChatService {
    private final SavingGroupChatMapper chatMapper;
    private final SavingGroupMapper savingGroupMapper;

    @Override
    @Transactional(readOnly = true)
    public long findRoomId(long savingGroupId, Long memberId) {
        requireLogin(memberId);
        Long roomId = chatMapper.findRoomId(savingGroupId);
        requireAccess(roomId, memberId);
        return roomId;
    }

    @Override
    @Transactional
    public SavingGroupChatMessage save(long roomId, Long memberId, String content) {
        requireAccess(roomId, memberId);
        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHAT_INVALID_CONTENT", "메시지를 1,000자 이내로 입력해 주세요.");
        }
        SavingGroupChatMessage message = new SavingGroupChatMessage();
        message.setRoomId(roomId);
        message.setSenderId(memberId);
        message.setContent(content.strip());
        chatMapper.insert(message);
        
        return chatMapper.findMessage(message.getMessageId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingGroupChatMessage> history(long roomId, Long memberId, Long beforeId, int size) {
        requireAccess(roomId, memberId);
        if (size < 1 || size > 100 || (beforeId != null && beforeId < 1)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHAT_INVALID_PAGE", "메시지 조회 범위를 확인해 주세요.");
        }
        return chatMapper.findHistory(roomId, beforeId, size);
    }

    // AI작성: 방 번호만 알고 있는 사용자가 대화를 열람하거나 저장할 수 없게 한다.
    private void requireAccess(Long roomId, Long memberId) {
        requireLogin(memberId);
        if (roomId == null || !savingGroupMapper.canAccessSavingGroupChat(roomId, memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CHAT_FORBIDDEN", "채팅방에 참여할 권한이 없습니다.");
        }
    }

    private void requireLogin(Long memberId) {
        if (memberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "로그인이 필요합니다.");
        }
    }
}
