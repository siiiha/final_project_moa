package com.nightstudy.moa.member.service.identity;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AccountRecoveryRequestStore {

    private static final String KEY_PREFIX = "moa:account-recovery:";

    private final StringRedisTemplate redisTemplate;

    public AccountRecoveryRequestStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String verificationId, String requestToken) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + verificationId + ":" + requestToken,
                "1",
                Duration.ofMinutes(5)
        );
    }

    public boolean consume(String verificationId, String requestToken) {
        return "1".equals(redisTemplate.opsForValue().getAndDelete(
                KEY_PREFIX + verificationId + ":" + requestToken
        ));
    }
}