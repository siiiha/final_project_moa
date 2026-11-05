package com.nightstudy.moa.chatbot.controller;

import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GuestChatbotRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_SECONDS = 60;

    // 첫 요청의 만료 시간 설정과 횟수 증가를 Redis에서 한 번에 처리

    private static final DefaultRedisScript<Long> INCREMENT = new
            DefaultRedisScript<>(
                """
                local count = redis.call('INCR', KEYS[1])
                if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return count
                """,
                Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public GuestChatbotRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void check(String remoteAddress) {
        Long count;
        try {
            count = redisTemplate.execute(
                    INCREMENT,
                    List.of("chatbot:guest:rate:" + remoteAddress),
                    Integer.toString(WINDOW_SECONDS)
            );
        } catch (DataAccessException exception) {
            throw unavailable();
        }
        if (count == null) {
            throw unavailable();
        }

        if (count > MAX_REQUESTS) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "CHATBOT_RATE_LIMITED",
                    "질문을 여러 번 보냈어요. 잠시 후 다시 시도해 주세요."
            );
        }
    }

    private ApiException unavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CHATBOT_UNAVAILABLE",
                "지금은 모아 챗봇을 이용할 수 없어요."
        );
    }
}
