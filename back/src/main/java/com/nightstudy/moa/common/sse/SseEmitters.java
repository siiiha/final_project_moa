package com.nightstudy.moa.common.sse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
// List: 한 회원이 여러 브라우저 탭을 열 수 있도록 연결 여러 개 저장
import java.util.Map;
// Map: 회원 번호와 SSE 연결 목록을 묶음
import java.util.concurrent.ConcurrentHashMap;
// ConcurrentHashMap: 여러 요청이 동시에 접근해도 안전한 Map
import java.util.concurrent.CopyOnWriteArrayList;
// CopyOnWriteArrayList: 연결 추가·삭제 중 충돌 방지
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseEmitters {
// SseEmitter: Spring의 실시간 이벤트 연결 객체

    private static final long TIMEOUT = Duration.ofMinutes(30).toMillis();   // 30분 뒤 연결 종료 → 브라우저가 다시 연결

    // 회원 번호 → 그 회원의 연결들 (탭을 여러 개 열면 연결도 여러 개)
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(memberId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결이 끝나면(정상 종료 · 시간 초과 · 오류) 목록에서 뺀다
        emitter.onCompletion(() -> remove(memberId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> remove(memberId, emitter));

        // 첫 이벤트. 보내야 응답 헤더가 바로 나가고, 브라우저가 "연결됨" 을 안다
        send(memberId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    // 한 회원의 모든 연결(탭)에 보낸다. 접속해 있지 않으면 아무 일도 없다 (DB 에는 이미 저장됨)
    public void send(Long memberId, String eventName, Long eventId, Object data) {
        List<SseEmitter> list = emitters.getOrDefault(memberId, List.of());
        for (SseEmitter emitter : list) {
            send(memberId, emitter, SseEmitter.event().id(String.valueOf(eventId)).name(eventName).data(data));
        }
    }

    // 30초마다 모든 연결에 주석(:ping)을 보낸다
    @Scheduled(fixedRate = 30_000)
    public void ping() {
        emitters.forEach((memberId, list) ->
                list.forEach(emitter -> send(memberId, emitter, SseEmitter.event().comment("ping"))));
    }

    private void send(Long memberId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);   // data(객체) 는 Jackson 이 JSON 으로 바꾼다
        } catch (IOException | IllegalStateException e) {
            // 브라우저가 이미 떠났다 (탭 닫기 · 새로고침). 정상적인 일이므로 목록에서 빼기만 한다
            remove(memberId, emitter);
        }
    }

    private void remove(Long memberId, SseEmitter emitter) {
        emitters.computeIfPresent(memberId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;   // 연결이 하나도 없으면 회원 번호도 지운다
        });
    }
}