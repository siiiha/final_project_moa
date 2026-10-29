package com.nightstudy.moa.common.config;

import com.nightstudy.moa.common.auth.JwtAuthenticationFilter;
import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.AuthSessionService;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final RevokedTokenMapper revokedTokenMapper;
    private final AuthSessionService authSessionService;

    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            SecurityErrorResponseWriter errorResponseWriter,
            RevokedTokenMapper revokedTokenMapper,
            AuthSessionService authSessionService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.errorResponseWriter = errorResponseWriter;
        this.revokedTokenMapper = revokedTokenMapper;
        this.authSessionService = authSessionService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        // AI작성: 회원 상태와 로그인 세션을 함께 검증하는 JWT 필터를 등록한다.
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter (
                        jwtTokenProvider,
                        errorResponseWriter,
                        revokedTokenMapper,
                        authSessionService
                );

        http
                // 쿠키 인증 대신 Authorization 헤더의 JWT 사용
                .csrf(AbstractHttpConfigurer::disable)

                // 기본 로그인 화면과 기본 인증 방식 사용 안 함
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 로그아웃은 별도 API에서 구현
                .logout(AbstractHttpConfigurer::disable)

                // 서버 세션에 로그인 상태를 저장하지 않음
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .requestCache(AbstractHttpConfigurer::disable)

                // API 접근 규칙
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS, "/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/social/signup",
                                "/api/auth/login",
                                "/api/auth/password/reset",
                                "/api/auth/check-email",
                                "/api/auth/check-nickname",
                                "/api/auth/email-verifications",
                                "/api/auth/email-verifications/verify",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/email-verifications/verify",
                                "/api/auth/email-verifications/find-account"
                        ).permitAll()

                        // AI작성: 소셜 인증 시작·콜백·결과 조회는 비로그인 상태에서 허용한다.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/kakao/login",
                                "/api/auth/kakao/callback",
                                "/api/auth/kakao/result",
                                "/api/auth/naver/login",
                                "/api/auth/naver/callback",
                                "/api/auth/naver/result"
                        ).permitAll()

                        // AI작성: 회원가입 전에도 약관과 활동 지역 목록을 조회할 수 있도록 허용한다.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/terms",
                                "/api/auth/regions"
                        ).permitAll()

                        // AI작성: 계모임 목록은 비로그인 사용자도 조회할 수 있다.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/saving-groups"
                        ).permitAll()

                        // AI작성: 숫자 ID 상세만 공개하고 지역·카테고리 등 다른 소모임 API는 기존 인증 정책을 유지한다.
                        .requestMatchers(
                                new RegexRequestMatcher("^/api/small-groups/\\d+$", HttpMethod.GET.name())
                        ).permitAll()

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/**"
                        ).authenticated()

                        .anyRequest().permitAll()
                )

                // 인증되지 않았거나 권한이 부족한 경우 JSON 응답
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, e) ->
                                        errorResponseWriter.write(
                                                response,
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "LOGIN_REQUIRED",
                                                "로그인이 필요합니다."
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, e) ->
                                        errorResponseWriter.write(
                                                response,
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "ACCESS_DENIED",
                                                "접근 권한이 없습니다."
                                        )
                        )
                )

                // JWT 필터 등록
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
