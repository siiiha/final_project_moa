package com.nightstudy.moa.common.auth;

import io.jsonwebtoken.Claims;
import java.util.UUID;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

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

    // 로그인 성공 시 토큰 발급
    public String createToken(Long memberId, String systemRole) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer("moa")
                .subject(memberId.toString())
                .claim("role", systemRole)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // 토큰의 서명과 만료 시간을 검사하고 내용 반환
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer("moa")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}