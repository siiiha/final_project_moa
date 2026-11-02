package com.nightstudy.moa.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisAuthStateCache implements AuthStateCache {

    private static final Logger log =
            LoggerFactory.getLogger(RedisAuthStateCache.class);

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String MEMBER_SESSIONS_KEY_PREFIX =
            "auth:member-sessions:";
    private static final String REVOKED_TOKEN_KEY_PREFIX =
            "auth:revoked-token:";

    private static final String SESSION_ID_FIELD = "sessionId";
    private static final String MEMBER_ID_FIELD = "memberId";
    private static final String SYSTEM_ROLE_FIELD = "systemRole";
    private static final String EXPIRES_AT_MS_FIELD = "expiresAtMs";

    private final StringRedisTemplate redisTemplate;

    public RedisAuthStateCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CacheLookup<Boolean> findRevokedToken(String tokenId) {
        if (!hasText(tokenId)) {
            return CacheLookup.miss();
        }

        try {
            if (Boolean.TRUE.equals(
                    redisTemplate.hasKey(revokedTokenKey(tokenId))
            )) {
                return CacheLookup.hit(true);
            }

            return CacheLookup.miss();
        } catch (RuntimeException e) {
            return unavailable("폐기 토큰 조회", e);
        }
    }

    @Override
    public CacheLookup<SessionState> findSession(String sessionId) {
        if (!hasText(sessionId)) {
            return CacheLookup.miss();
        }

        try {
            Map<Object, Object> values = redisTemplate.opsForHash().entries(
                    sessionKey(sessionId)
            );

            if (values.isEmpty()) {
                return CacheLookup.miss();
            }

            SessionState sessionState = toSessionState(values);

            // AI작성: 만료되었거나 훼손된 캐시 값은 인증에 사용하지 않고 MySQL 조회로 대체한다.
            if (sessionState == null
                    || sessionState.expiresAtMs() <= System.currentTimeMillis()) {
                redisTemplate.delete(sessionKey(sessionId));
                return CacheLookup.miss();
            }

            return CacheLookup.hit(sessionState);
        } catch (RuntimeException e) {
            return unavailable("세션 조회", e);
        }
    }

    @Override
    public boolean saveSession(SessionState sessionState) {
        if (!isValidSessionState(sessionState)) {
            return false;
        }

        Duration remainingTtl = remainingTtl(sessionState.expiresAtMs());

        if (remainingTtl == null) {
            return false;
        }

        try {
            String sessionKey = sessionKey(sessionState.sessionId());
            String memberSessionsKey = memberSessionsKey(
                    sessionState.memberId()
            );

            redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                    SESSION_ID_FIELD, sessionState.sessionId(),
                    MEMBER_ID_FIELD, String.valueOf(sessionState.memberId()),
                    SYSTEM_ROLE_FIELD, sessionState.systemRole(),
                    EXPIRES_AT_MS_FIELD,
                    String.valueOf(sessionState.expiresAtMs())
            ));
            redisTemplate.expire(sessionKey, remainingTtl);
            redisTemplate.opsForSet().add(
                    memberSessionsKey,
                    sessionState.sessionId()
            );
            extendMemberSessionsTtl(memberSessionsKey, remainingTtl);

            return true;
        } catch (RuntimeException e) {
            logCacheFailure("세션 저장", e);
            return false;
        }
    }

    @Override
    public boolean removeSession(String sessionId) {
        if (!hasText(sessionId)) {
            return false;
        }

        try {
            Object memberIdValue = redisTemplate.opsForHash().get(
                    sessionKey(sessionId),
                    MEMBER_ID_FIELD
            );

            redisTemplate.delete(sessionKey(sessionId));

            Long memberId = toLong(memberIdValue);
            if (memberId != null && memberId > 0) {
                redisTemplate.opsForSet().remove(
                        memberSessionsKey(memberId),
                        sessionId
                );
            }

            return true;
        } catch (RuntimeException e) {
            logCacheFailure("세션 삭제", e);
            return false;
        }
    }

    @Override
    public boolean removeMemberSessions(Long memberId) {
        if (memberId == null || memberId <= 0) {
            return false;
        }

        try {
            String memberSessionsKey = memberSessionsKey(memberId);
            Set<String> sessionIds = redisTemplate.opsForSet().members(
                    memberSessionsKey
            );

            if (sessionIds != null && !sessionIds.isEmpty()) {
                List<String> sessionKeys = sessionIds.stream()
                        .map(this::sessionKey)
                        .toList();
                redisTemplate.delete(sessionKeys);
            }

            redisTemplate.delete(memberSessionsKey);
            return true;
        } catch (RuntimeException e) {
            logCacheFailure("회원 세션 삭제", e);
            return false;
        }
    }

    @Override
    public boolean saveRevokedToken(String tokenId, long expiresAtMs) {
        if (!hasText(tokenId)) {
            return false;
        }

        Duration remainingTtl = remainingTtl(expiresAtMs);

        if (remainingTtl == null) {
            return false;
        }

        try {
            redisTemplate.opsForValue().set(
                    revokedTokenKey(tokenId),
                    "1",
                    remainingTtl
            );
            return true;
        } catch (RuntimeException e) {
            logCacheFailure("폐기 토큰 저장", e);
            return false;
        }
    }

    private void extendMemberSessionsTtl(
            String memberSessionsKey,
            Duration remainingTtl
    ) {
        Long currentTtlMs = redisTemplate.getExpire(
                memberSessionsKey,
                TimeUnit.MILLISECONDS
        );

        if (currentTtlMs == null
                || currentTtlMs < remainingTtl.toMillis()) {
            redisTemplate.expire(memberSessionsKey, remainingTtl);
        }
    }

    private SessionState toSessionState(Map<Object, Object> values) {
        String sessionId = toText(values.get(SESSION_ID_FIELD));
        Long memberId = toLong(values.get(MEMBER_ID_FIELD));
        String systemRole = toText(values.get(SYSTEM_ROLE_FIELD));
        Long expiresAtMs = toLong(values.get(EXPIRES_AT_MS_FIELD));

        if (!hasText(sessionId)
                || memberId == null
                || !hasText(systemRole)
                || expiresAtMs == null) {
            return null;
        }

        return new SessionState(
                sessionId,
                memberId,
                systemRole,
                expiresAtMs
        );
    }

    private boolean isValidSessionState(SessionState sessionState) {
        return sessionState != null
                && hasText(sessionState.sessionId())
                && sessionState.memberId() != null
                && sessionState.memberId() > 0
                && ("USER".equals(sessionState.systemRole())
                || "ADMIN".equals(sessionState.systemRole()));
    }

    private Duration remainingTtl(long expiresAtMs) {
        long remainingMs = expiresAtMs - System.currentTimeMillis();

        if (remainingMs <= 0) {
            return null;
        }

        return Duration.ofMillis(remainingMs);
    }

    private <T> CacheLookup<T> unavailable(
            String action,
            RuntimeException e
    ) {
        logCacheFailure(action, e);
        return CacheLookup.unavailable();
    }

    private void logCacheFailure(String action, RuntimeException e) {
        // AI작성: 토큰·세션 식별자는 로그에 남기지 않고 캐시 실패 원인만 기록한다.
        log.warn(
                "인증 캐시 {} 실패. MySQL 인증 조회로 대체합니다. cause={}",
                action,
                e.getClass().getSimpleName()
        );
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String memberSessionsKey(Long memberId) {
        return MEMBER_SESSIONS_KEY_PREFIX + memberId;
    }

    private String revokedTokenKey(String tokenId) {
        return REVOKED_TOKEN_KEY_PREFIX + tokenId;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toText(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
