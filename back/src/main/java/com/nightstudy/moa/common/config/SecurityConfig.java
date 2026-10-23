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
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.mapper.MemberMapper;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final RevokedTokenMapper revokedTokenMapper;

    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            SecurityErrorResponseWriter errorResponseWriter,
            RevokedTokenMapper revokedTokenMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.errorResponseWriter = errorResponseWriter;
        this.revokedTokenMapper = revokedTokenMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MemberMapper memberMapper
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter (
                        jwtTokenProvider,
                        errorResponseWriter,
                        revokedTokenMapper,
                        memberMapper
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
                                "/api/auth/login",
                                "/api/auth/check-email",
                                "/api/auth/check-nickname",
                                "/api/auth/email-verifications",
                                "/api/auth/email-verifications/verify"
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