package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupChatMapper;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.member.service.restriction.MemberFeatureRestrictionService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatRoomResponse;

@Service
public class SavingGroupChatServiceImpl implements SavingGroupChatService {
    private final SavingGroupChatMapper chatMapper;
    private final SavingGroupMapper savingGroupMapper;

    // AI작성: 채팅 메시지 전송 전 회원의 채팅 이용 제한 여부를 검사한다.
    private final MemberFeatureRestrictionService restrictionService;

    // AI작성: 단일 서버에서 연결과 무관하게 회원·채팅방별 발송 기록을 관리한다.
    private final Clock clock;
    private final Map<LimitKey, LimitState> limits = new HashMap<>();

    @Autowired
    public SavingGroupChatServiceImpl(SavingGroupChatMapper chatMapper, SavingGroupMapper savingGroupMapper,
            MemberFeatureRestrictionService restrictionService) {
        this(chatMapper, savingGroupMapper, restrictionService, Clock.systemUTC());
    }

    // AI작성: 테스트에서 10초·180초 경계를 실제 대기 없이 검증한다.
    SavingGroupChatServiceImpl(SavingGroupChatMapper chatMapper, SavingGroupMapper savingGroupMapper,
            MemberFeatureRestrictionService restrictionService, Clock clock) {
        this.chatMapper = chatMapper;
        this.savingGroupMapper = savingGroupMapper;
        this.restrictionService = restrictionService;
        this.clock = clock;
    }

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

        return roomResponse(roomId, memberId, chatRestricted);
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
        // AI작성: 유효한 발송 시도를 먼저 기록하며 차단된 메시지는 저장하지 않는다.
        validateRateLimit(roomId, memberId);
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

    // AI작성: 모든 상태 접근을 같은 잠금으로 보호해 다중 탭·동시 발송 우회를 막는다.
    private synchronized void validateRateLimit(long roomId, long memberId) {
        Instant now = clock.instant();
        cleanExpired(now);
        LimitState state = limits.computeIfAbsent(new LimitKey(roomId, memberId), key -> new LimitState());
        if (state.until != null) throw rateLimited();
        if (state.sent.size() >= 5) {
            state.until = now.plusSeconds(180);
            state.sent.clear();
            throw rateLimited();
        }
        state.sent.addLast(now);
    }

    private synchronized SavingGroupChatRoomResponse roomResponse(long roomId, long memberId, boolean restricted) {
        Instant now = clock.instant();
        cleanExpired(now);
        LimitState state = limits.get(new LimitKey(roomId, memberId));
        Instant until = state == null ? null : state.until;
        return new SavingGroupChatRoomResponse(roomId, restricted, until != null, until, now);
    }

    // AI작성: 10초 경계의 기록은 제외하고 만료된 제한은 제거한다. 제한 중 시도는 만료를 연장하지 않는다.
    private void cleanExpired(Instant now) {
        limits.values().removeIf(state -> {
            while (!state.sent.isEmpty() && !state.sent.peekFirst().isAfter(now.minusSeconds(10))) {
                state.sent.removeFirst();
            }
            if (state.until != null && !now.isBefore(state.until)) state.until = null;
            return state.until == null && state.sent.isEmpty();
        });
    }

    private ApiException rateLimited() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CHAT_RATE_LIMITED",
                "짧은 시간에 메시지를 많이 보내 채팅 발송이 3분간 제한되었습니다.");
    }

    private record LimitKey(long roomId, long memberId) {}

    private static class LimitState {
        private final ArrayDeque<Instant> sent = new ArrayDeque<>();
        private Instant until;
    }
}
