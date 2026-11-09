package com.nightstudy.moa.common.config;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class SavingGroupWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Pattern ROOM_SEND = Pattern.compile("/app/chat/rooms/([1-9][0-9]{0,18})");
    private static final Pattern ROOM_TOPIC = Pattern.compile("/topic/chat/rooms/([1-9][0-9]{0,18})");
    private final JwtTokenProvider jwtTokenProvider;
    private final RevokedTokenMapper revokedTokenMapper;
    private final AuthSessionService authSessionService;
    private final SavingGroupMapper savingGroupMapper;

    @Value("${moa.chat.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // AI작성: 핸드셰이크 이후 CONNECT에서 인증하며 내부 예외는 클라이언트에 공개하지 않는다.
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
        registry.setErrorHandler(new StompSubProtocolErrorHandler() {
            @Override
            public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> message, Throwable error) {
                Throwable cause = error;
                while (cause != null && !(cause instanceof ChatAccessException)) {
                    cause = cause.getCause();
                }
                StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
                headers.setMessage(cause == null ? "CHAT_CONNECTION_ERROR" : cause.getMessage());
                return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (headers == null || headers.getCommand() == null) {
                    return message;
                }
                switch (headers.getCommand()) {
                    case CONNECT, STOMP -> headers.setUser(authenticate(headers.getFirstNativeHeader("Authorization")));
                    case SUBSCRIBE -> checkDestination(headers, ROOM_TOPIC);
                    case SEND -> checkDestination(headers, ROOM_SEND);
                    default -> { }
                }
                return message;
            }
        });
    }

    // AI작성: HTTP 인증과 같은 토큰·세션 검증을 STOMP 연결 시 수행한다.
    private Principal authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ChatAccessException("AUTH_REQUIRED");
        }
        try {
            Claims claims = jwtTokenProvider.parseToken(authorization.substring(7));
            long memberId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            String tokenId = claims.getId();
            if (memberId <= 0 || claims.getExpiration() == null || tokenId == null || tokenId.isBlank()
                    || (!"USER".equals(role) && !"ADMIN".equals(role))
                    || revokedTokenMapper.countByTokenId(tokenId) > 0
                    || !authSessionService.isAccessAllowed(memberId, role, claims.get("sid", String.class),
                            tokenId, claims.getExpiration().getTime())) {
                throw new ChatAccessException("TOKEN_INVALID");
            }
            return () -> Long.toString(memberId);
        } catch (ExpiredJwtException exception) {
            throw new ChatAccessException("TOKEN_EXPIRED");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ChatAccessException("TOKEN_INVALID");
        }
    }

    // AI작성: 허용 경로를 제한해 브로커 직접 발행과 타 계모임 대화 열람을 차단한다.
    private void checkDestination(StompHeaderAccessor headers, Pattern roomPath) {
        Principal user = headers.getUser();
        if (user == null) {
            throw new ChatAccessException("AUTH_REQUIRED");
        }
        String destination = headers.getDestination();
        if (roomPath == ROOM_TOPIC && "/user/queue/errors".equals(destination)) {
            return;
        }
        Matcher match = roomPath.matcher(destination == null ? "" : destination);
        if (match.matches()) {
            try {
                if (savingGroupMapper.canAccessSavingGroupChat(
                        Long.parseLong(match.group(1)), Long.parseLong(user.getName()))) {
                    return;
                }
            } catch (NumberFormatException exception) {
                throw new ChatAccessException("CHAT_FORBIDDEN");
            }
        }
        throw new ChatAccessException("CHAT_FORBIDDEN");
    }

    private static class ChatAccessException extends RuntimeException {
        ChatAccessException(String code) {
            super(code);
        }
    }
}
