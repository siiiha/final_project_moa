package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.common.auth.SocialSignupSessionManager.PendingSignup;
import com.nightstudy.moa.member.dto.MemberAuthTokenDto;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.dto.SocialSignupRequest;
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.member.service.SocialSignupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 일반·소셜 로그인에서 세션과 쿠키 발급을 공통 처리
@Component
public class LoginSessionManager {

    private final AuthSessionService authSessionService;
    private final RefreshTokenCookie refreshTokenCookie;
    private final SocialSignupService socialSignupService;

    public LoginSessionManager(
            AuthSessionService authSessionService,
            RefreshTokenCookie refreshTokenCookie,
            SocialSignupService socialSignupService
    ) {
        this.authSessionService = authSessionService;
        this.refreshTokenCookie = refreshTokenCookie;
        this.socialSignupService = socialSignupService;
    }

    public String completeLogin(
            String accessToken,
            HttpServletResponse response
    ) {
        MemberAuthTokenDto tokens =
                authSessionService.startSession(accessToken);

        refreshTokenCookie.write(
                response,
                tokens.getRefreshToken(),
                tokens.getExpiresAtMs()
        );

        return tokens.getAccessToken();
    }

    public SocialLoginResultDto completeLogin(
            SocialLoginResultDto result,
            HttpServletResponse response
    ) {
        if (!result.isSignupRequired()) {
            result.setToken(
                    completeLogin(result.getToken(), response)
            );
        }

        return result;
    }

    // 가입 저장이나 세션 발급이 실패하면 함께 롤백
    @Transactional
    public SocialLoginResultDto completeSignup(
            SocialSignupRequest request,
            PendingSignup pending,
            HttpServletResponse response
    ) {
        SocialLoginResultDto result =
                socialSignupService.signup(request, pending);

        return completeLogin(result, response);
    }
}