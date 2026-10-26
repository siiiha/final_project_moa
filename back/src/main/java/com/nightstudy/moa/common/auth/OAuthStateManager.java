package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

// 소셜 로그인 요청과 콜백이 같은 브라우저에서 시작됐는지 검증
@Component
public class OAuthStateManager {

    private static final String KEY_PREFIX = "OAUTH_STATE_";
    private static final long VALID_SECONDS = 300;

    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(HttpServletRequest request, String provider) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        HttpSession session = request.getSession(true);
        String key = KEY_PREFIX + provider;

        // 제공사별 최신 요청만 보관하여 유효시간은 5분
        synchronized (session) {
            session.setAttribute(key, state);
            session.setAttribute(
                    key + "_EXPIRES",
                    Instant.now().plusSeconds(VALID_SECONDS)
            );
        }

        return state;
    }

    public void validateAndConsume(
            HttpServletRequest request,
            String provider,
            String receivedState
    ) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw invalidState();
        }

        String key = KEY_PREFIX + provider;

        synchronized (session) {
            Object storedState = session.getAttribute(key);
            Object storedExpires = session.getAttribute(key + "_EXPIRES");

            // 검증에 사용한 요청 즉시 제거해 재사용 막음
            session.removeAttribute(key);
            session.removeAttribute(key + "_EXPIRES");

            if (!(storedState instanceof String expectedState)
                    || !(storedExpires instanceof Instant expiresAt)
                    || receivedState == null
                    || receivedState.length() != 43
                    || !Instant.now().isBefore(expiresAt)) {
                throw invalidState();
            }

            boolean matches = MessageDigest.isEqual(
                    expectedState.getBytes(StandardCharsets.UTF_8),
                    receivedState.getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                throw invalidState();
            }
        }
    }

    private ApiException invalidState() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_OAUTH_STATE",
                "소셜 로그인 요청이 만료되었거나 유효하지 않습니다. 다시 시도해주세요."
        );
    }
}