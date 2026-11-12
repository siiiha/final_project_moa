package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.member.service.restriction.MemberFeatureRestrictionService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupChatMapper;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// AI작성: 연결 상태와 무관한 도배 차단·시간 경계·동시성·기존 제재를 DB 없이 확인한다.
class SavingGroupChatServiceTest {
    private final SavingGroupChatMapper chat = mock(SavingGroupChatMapper.class);
    private final SavingGroupMapper groups = mock(SavingGroupMapper.class);
    private final MemberFeatureRestrictionService restrictions = mock(MemberFeatureRestrictionService.class);
    private final Clock clock = mock(Clock.class);
    private final Instant start = Instant.parse("2026-11-11T00:00:00Z");
    private SavingGroupChatServiceImpl service;

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(start);
        when(groups.canAccessSavingGroupChat(anyLong(), anyLong())).thenReturn(true);
        when(chat.findRoomId(20L)).thenReturn(10L);
        doAnswer(invocation -> {
            invocation.<SavingGroupChatMessage>getArgument(0).setMessageId(1L);
            return null;
        }).when(chat).insert(any());
        service = new SavingGroupChatServiceImpl(chat, groups, restrictions, clock);
    }

    private void sendFive() {
        for (int i = 0; i < 5; i++) service.save(10, 30L, "안녕하세요");
    }

    private void blocked() {
        assertThatThrownBy(() -> service.save(10, 30L, "차단 메시지"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("CHAT_RATE_LIMITED");
                    assertThat(e.getStatus().value()).isEqualTo(429);
                });
    }

    @Test
    void sixthAttemptIsNotSavedAndReconnectDoesNotResetOrExtendLimit() {
        sendFive();
        blocked();
        var room = service.findRoom(20, 30L);
        assertThat(room.spamRestricted()).isTrue();
        assertThat(room.restrictedUntil()).isEqualTo(start.plusSeconds(180));
        assertThat(room.serverTime()).isEqualTo(start);
        when(clock.instant()).thenReturn(start.plusMillis(179999));
        blocked();
        assertThat(service.findRoom(20, 30L).restrictedUntil()).isEqualTo(room.restrictedUntil());
        verify(chat, times(5)).insert(any());
        when(clock.instant()).thenReturn(start.plusSeconds(180));
        assertThat(service.findRoom(20, 30L).spamRestricted()).isFalse();
        assertThat(service.findRoom(20, 30L).restrictedUntil()).isNull();
        service.save(10, 30L, "다시 발송");
        verify(chat, times(6)).insert(any());
    }

    @Test
    void tenSecondBoundaryAndInvalidContentDoNotTriggerLimit() {
        assertThatThrownBy(() -> service.save(10, 30L, " ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save(10, 30L, "가".repeat(1001))).isInstanceOf(ApiException.class);
        sendFive();
        when(clock.instant()).thenReturn(start.plusSeconds(10));
        service.save(10, 30L, "경계 이후");
        assertThat(service.findRoom(20, 30L).spamRestricted()).isFalse();
        verify(chat, times(6)).insert(any());
    }

    @Test
    void rollingWindowStillDetectsFloodJustBeforeTenSeconds() {
        sendFive();
        when(clock.instant()).thenReturn(start.plusMillis(9999));
        blocked();
        var room = service.findRoom(20, 30L);
        assertThat(room.restrictedUntil()).isEqualTo(start.plusMillis(189999));
        verify(chat, times(5)).insert(any());
    }

    @Test
    void otherMembersAndRoomsRemainIndependent() {
        sendFive();
        blocked();
        service.save(10, 31L, "다른 회원");
        service.save(11, 30L, "다른 방");
        assertThat(service.findRoom(20, 31L).spamRestricted()).isFalse();
        verify(chat, times(7)).insert(any());
    }

    @Test
    void adminRestrictionStillBlocksAfterSpamLimitExpires() {
        sendFive();
        blocked();
        when(clock.instant()).thenReturn(start.plusSeconds(180));
        when(restrictions.isRestricted(30L, MemberRestrictionScope.CHAT)).thenReturn(true);
        doThrow(new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "FEATURE_RESTRICTED", "제재"))
                .when(restrictions).validateAllowed(30L, MemberRestrictionScope.CHAT);
        var room = service.findRoom(20, 30L);
        assertThat(room.chatRestricted()).isTrue();
        assertThat(room.spamRestricted()).isFalse();
        assertThatThrownBy(() -> service.save(10, 30L, "차단"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("FEATURE_RESTRICTED"));
        verify(chat, times(5)).insert(any());
    }

    @Test
    void unauthorizedRequestsNeverReachStorage() {
        assertThatThrownBy(() -> service.save(10, null, "로그인 필요")).isInstanceOf(ApiException.class);
        when(groups.canAccessSavingGroupChat(10L, 30L)).thenReturn(false);
        assertThatThrownBy(() -> service.save(10, 30L, "권한 없음")).isInstanceOf(ApiException.class);
        verify(chat, never()).insert(any());
    }

    @Test
    void concurrentAttemptsAllowOnlyFiveMessages() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch ready = new CountDownLatch(12);
        CountDownLatch go = new CountDownLatch(1);
        try {
            var tasks = new java.util.ArrayList<Future<Boolean>>();
            for (int i = 0; i < 12; i++) tasks.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try { service.save(10, 30L, "동시 발송"); return true; }
                catch (ApiException e) {
                    assertThat(e.getCode()).isEqualTo("CHAT_RATE_LIMITED");
                    return false;
                }
            }));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            int accepted = 0;
            for (var task : tasks) if (task.get(5, TimeUnit.SECONDS)) accepted++;
            assertThat(accepted).isEqualTo(5);
            verify(chat, times(5)).insert(any(SavingGroupChatMessage.class));
        } finally { go.countDown(); pool.shutdownNow(); }
    }
}
