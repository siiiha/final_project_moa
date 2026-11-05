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
    //emitters = new HashMap<>(); : final 재할당 불가능
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(Long memberId) {
        // AI작성: memberId는 JWT 인증을 거쳐 확인된 로그인 회원 번호를 Controller와 Service를 통해 전달받습니다.
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        // AI작성: 회원의 연결 목록이 없으면 새로 만들고, 현재 브라우저의 SSE 연결을 목록에 추가합니다.
        // 회원 연결 목록  : 그 회원이 현재 열어 둔 브라우저·탭별 SSE 연결들을 모아 둔 List
        emitters.computeIfAbsent(memberId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(memberId, emitter));
        // 정상 종료 → 목록에서 제거
        emitter.onTimeout(emitter::complete);
        // 시간 초과 → 연결 종료
        emitter.onError(e -> remove(memberId, emitter));
        // 오류 발생 → 목록에서 제거

        // 첫 이벤트. 보내야 응답 헤더가 바로 나가고, 브라우저가 "연결됨" 을 안다
        send(memberId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    // 첫 번째 send()는 특정 회원의 연결 목록을 찾아서 모든 연결에 보내는 역할
    // 실제 알림 내용을 받아서 SSE 이벤트로 조립
    // 접속해 있지 않으면 아무 일도 없다 (DB 에는 이미 저장됨)
    public void send(Long memberId, String eventName, Long eventId, Object data) {
        List<SseEmitter> list = emitters.getOrDefault(memberId, List.of());
        // getOrDefault : 값이 없으면 기본값을 돌려주는 Java의 Map 인터페이스에 기본으로 들어 있는 메서드
        for (SseEmitter emitter : list) {
            //한 회원이 여러 탭이나 기기로 접속할 수 있기 때문에 모든 연결에 보내려고 반복문을 사용
            send(memberId, emitter, SseEmitter.event().id(String.valueOf(eventId)).name(eventName).data(data));
        }
    }

    // AI작성: 재연결 복구 알림은 같은 회원의 다른 탭이 아닌 새로 연결된 탭에만 전송합니다.
    public void send(
            Long memberId,
            SseEmitter emitter,
            String eventName,
            Long eventId,
            Object data
    ) {
        send(
                memberId,
                emitter,
                SseEmitter.event()
                        .id(String.valueOf(eventId))
                        .name(eventName)
                        .data(data)
        );
    }

    // 30초마다 모든 연결에 주석(:ping)을 보낸다
    @Scheduled(fixedRate = 30_000)
    public void ping() {
        emitters.forEach((memberId, list) ->
                list.forEach(emitter -> send(memberId, emitter, SseEmitter.event().comment("ping"))));
    }

    // 메서드 오버로딩~! : 실제 전송과 실패 처리를 공통으로 재사용하기 위해서
    // 두 번째 send()는 연결 하나에 실제로 전송하는 역할
    // 만들어진 이벤트를 특정 브라우저 연결 하나에 실제 전송
    private void send(Long memberId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);   // data(객체) 는 Jackson 이 JSON 으로 바꾼다
        } catch (IOException | IllegalStateException e) {
            // 브라우저가 이미 떠났다 (탭 닫기 · 새로고침). 정상적인 일이므로 목록에서 빼기만 한다
            remove(memberId, emitter);
        }
    }

    // 끊어진 SSE 연결을 회원의 연결 목록에서 제거하는 코드
    private void remove(Long memberId, SseEmitter emitter) {
        emitters.computeIfPresent(memberId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;   // 연결이 하나도 없으면 회원 번호도 지운다
        });
    }
}
