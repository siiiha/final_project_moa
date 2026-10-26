package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

// 소셜 인증이 끝난 사용자의 추가 가입 정보를 잠시 보관
@Component
public class SocialSignupSessionManager {

    private static final String SESSION_KEY = "SOCIAL_PENDING_SIGNUP";
    private static final long VALID_SECONDS = 600;

    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(
            HttpServletRequest request,
            String provider,
            String providerUserId
    ) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        HttpSession session = request.getSession(true);

        // 소셜 인증 완료 후 세션 번호를 교체
        request.changeSessionId();

        PendingSignup pending = new PendingSignup(
                provider,
                providerUserId,
                hash(token),
                Instant.now().plusSeconds(VALID_SECONDS)
        );

        synchronized (session) {
            session.setAttribute(SESSION_KEY, pending);
        }

        return token;
    }

    public PendingSignup requirePendingSignup(
            HttpServletRequest request,
            String token
    ) {
        HttpSession session = request.getSession(false);

        if (session == null || token == null || token.length() != 43) {
            throw invalidSignup();
        }

        synchronized (session) {
            Object stored = session.getAttribute(SESSION_KEY);

            if (!(stored instanceof PendingSignup pending)) {
                throw invalidSignup();
            }

            if (!Instant.now().isBefore(pending.expiresAt())) {
                session.removeAttribute(SESSION_KEY);
                throw invalidSignup();
            }

            // 같은 브라우저에 보관된 가입 토큰과 일치해야 함
            boolean matches = MessageDigest.isEqual(
                    pending.tokenHash().getBytes(StandardCharsets.UTF_8),
                    hash(token).getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                throw invalidSignup();
            }

            return pending;
        }
    }

    public void clear(
            HttpServletRequest request,
            PendingSignup expected
    ) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        synchronized (session) {
            // AI작성: 가입 완료에 사용한 정보만 제거한다.
            if (expected.equals(session.getAttribute(SESSION_KEY))) {
                session.removeAttribute(SESSION_KEY);
            }
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "가입 토큰 검증 기능을 초기화하지 못했습니다."
            );
        }
    }

    private ApiException invalidSignup() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_SOCIAL_SIGNUP",
                "소셜 가입 요청이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요."
        );
    }

    // 제공사 정보는 서버가 확인한 값으로만 보관
    public record PendingSignup(
            String provider,
            String providerUserId,
            String tokenHash,
            Instant expiresAt
    ) {
    }
}