package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

// 소셜 인증 결과를 같은 브라우저의 프론트에 한 번만 전달
@Component
public class SocialLoginResultManager {

    private static final String SESSION_KEY = "SOCIAL_LOGIN_RESULT";
    private static final long VALID_SECONDS = 120;

    public void save(
            HttpServletRequest request,
            SocialLoginResultDto result
    ) {
        HttpSession session = request.getSession(true);

        // 인증 완료 후 세션 번호를 교체하며 기존 가입 정보는 유지
        request.changeSessionId();

        synchronized (session) {
            session.setAttribute(
                    SESSION_KEY,
                    new StoredResult(
                            result,
                            Instant.now().plusSeconds(VALID_SECONDS)
                    )
            );
        }
    }

    public SocialLoginResultDto consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw invalidResult();
        }

        synchronized (session) {
            Object stored = session.getAttribute(SESSION_KEY);

            // 결과는 한 번만 받아갈 수 있음
            session.removeAttribute(SESSION_KEY);

            if (!(stored instanceof StoredResult saved)
                    || !Instant.now().isBefore(saved.expiresAt())) {
                throw invalidResult();
            }

            return saved.result();
        }
    }

    private ApiException invalidResult() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "SOCIAL_LOGIN_RESULT_EXPIRED",
                "로그인 결과가 만료되었거나 이미 처리되었습니다. 다시 로그인해주세요."
        );
    }

    // 인증 결과와 만료 시각은 서버 세션에만 보관
    private record StoredResult(
            SocialLoginResultDto result,
            Instant expiresAt
    ) {
    }
}