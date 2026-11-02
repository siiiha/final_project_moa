package com.nightstudy.moa.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    // 기존 로그인 코드도 같은 발급 로직을 재사용
    public String createToken(Long memberId, String systemRole) {
        return createToken(memberId, systemRole, null);
    }

    // 로그인 세션 번호를 포함한 access token을 발급
    public String createToken(
            Long memberId,
            String systemRole,
            String sessionId
    ) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer("moa")
                .subject(memberId.toString())
                .claim("role", systemRole)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key, Jwts.SIG.HS256);

        // 세션 번호로 로그아웃·재사용 감지 시 접근을 차단
        if (sessionId != null) {
            builder.claim("sid", sessionId);
        }

        return builder.compact();
    }

    // 토큰의 서명과 만료 시간을 검사하고 내용을 반환
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer("moa")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}