package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupChatMapper;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.member.service.restriction.MemberFeatureRestrictionService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatRoomResponse;

@Service
@RequiredArgsConstructor
public class SavingGroupChatServiceImpl implements SavingGroupChatService {
    private final SavingGroupChatMapper chatMapper;
    private final SavingGroupMapper savingGroupMapper;
    private final NotificationService notificationService;

    // AI작성: 채팅 메시지 전송 전 회원의 채팅 이용 제한 여부를 검사한다.
    private final MemberFeatureRestrictionService restrictionService;

    @Override
    @Transactional(readOnly = true)
// AI작성: 채팅방 정보와 현재 회원의 채팅 제한 상태를 함께 반환한다.
    public SavingGroupChatRoomResponse findRoom(
            long savingGroupId,
            Long memberId
    ) {
        requireLogin(memberId);

        Long roomId = chatMapper.findRoomId(savingGroupId);
        requireAccess(roomId, memberId);

        boolean chatRestricted = restrictionService.isRestricted(
                memberId,
                MemberRestrictionScope.CHAT
        );

        return new SavingGroupChatRoomResponse(
                roomId,
                chatRestricted
        );
    }

    @Override
    @Transactional
    public SavingGroupChatMessage save(long roomId, Long memberId, String content) {
        requireAccess(roomId, memberId);

        // AI작성: 활성화된 채팅 제재가 있으면 메시지 저장과 전송을 차단한다.
        restrictionService.validateAllowed(
                memberId,
                MemberRestrictionScope.CHAT
        );

        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHAT_INVALID_CONTENT", "메시지를 1,000자 이내로 입력해 주세요.");
        }
        SavingGroupChatMessage message = new SavingGroupChatMessage();
        message.setRoomId(roomId);
        message.setSenderId(memberId);
        message.setContent(content.strip());
        chatMapper.insert(message);
        Long savingGroupId = chatMapper.findSavingGroupId(roomId);
        if (savingGroupId == null) {
            throw new IllegalStateException("채팅방의 계모임 정보를 확인할 수 없습니다.");
        }
        // AI작성: 메시지 저장 성공 후 발신자를 제외한 현재 채팅방 참여자에게 새 메시지 알림을 보낸다.
        chatMapper.findNotificationReceiverIds(roomId, memberId)
                .forEach(receiverId -> notificationService.send(
                        receiverId,
                        NotificationType.CHAT_MESSAGE_CREATED,
                        "계모임 채팅방에 새 메시지가 도착했습니다.",
                        "/saving-groups/" + savingGroupId
                ));
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
