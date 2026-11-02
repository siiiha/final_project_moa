package com.nightstudy.moa.common.auth;

/**
 * 인증 상태를 빠르게 확인하기 위한 캐시의 공통 계약이다.
 *
 * <p>MySQL은 인증 상태의 최종 기준으로 유지하고, 구현체는 Valkey 장애 또는 캐시 미스를
 * 구분해 호출자가 MySQL 조회로 안전하게 대체할 수 있도록 한다.</p>
 */
public interface AuthStateCache {

    CacheLookup<Boolean> findRevokedToken(String tokenId);

    CacheLookup<SessionState> findSession(String sessionId);

    boolean saveSession(SessionState sessionState);

    boolean removeSession(String sessionId);

    boolean removeMemberSessions(Long memberId);

    boolean saveRevokedToken(String tokenId, long expiresAtMs);

    // AI작성: 캐시 값 존재, 값 없음, 저장소 장애를 분리해 인증의 MySQL 대체 여부를 판단한다.
    record CacheLookup<T>(Status status, T value) {

        public enum Status {
            HIT,
            MISS,
            UNAVAILABLE
        }

        public static <T> CacheLookup<T> hit(T value) {
            return new CacheLookup<>(Status.HIT, value);
        }

        public static <T> CacheLookup<T> miss() {
            return new CacheLookup<>(Status.MISS, null);
        }

        public static <T> CacheLookup<T> unavailable() {
            return new CacheLookup<>(Status.UNAVAILABLE, null);
        }

        public boolean isHit() {
            return status == Status.HIT;
        }
    }

    // AI작성: JWT와 대조할 세션의 최소 인증 정보만 보관하고 Refresh Token 원문은 저장하지 않는다.
    record SessionState(
            String sessionId,
            Long memberId,
            String systemRole,
            long expiresAtMs
    ) {
    }
}
