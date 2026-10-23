package com.nightstudy.moa.anomaly;
import com.nightstudy.moa.anomaly.dto.AnomalyHistoryRequest;
import com.nightstudy.moa.anomaly.service.AnomalyServiceImpl;
import com.nightstudy.moa.common.exception.ApiException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// AI작성: 로컬 가짜 HTTP 서버로 장애를 재현한다. 실제 금융 API·DB에는 접속하지 않는다.
class AnomalyFaultTest {
    AnomalyHistoryRequest sample() {
        return new AnomalyHistoryRequest("10013","2",List.of(new AnomalyHistoryRequest.Transaction(
            "2","10013","WITHDRAWAL","50000.00","2026-09-18T20:08:00+09:00","1000000.00",2L)));
    }
    String valid() {
        return """
        {"account_id":"10013","transaction_id":"2","transacted_at":"2026-09-18T20:08:00+09:00",
         "transaction_type":"WITHDRAWAL","anomaly_score":0.7,"threshold":0.6,"is_anomaly":true,
         "model_version":"test-version","feature_schema_version":"moa-anomaly-features-v1",
         "cold_start":false,"quality_flags":[],"requires_review":false}
        """;
    }
    static class Stub implements AutoCloseable {
        final HttpServer server;
        final ExecutorService executor=Executors.newCachedThreadPool();
        final AtomicReference<String> body=new AtomicReference<>();
        final int delay;
        Stub(String content,int delay) throws Exception {
            body.set(content);this.delay=delay;
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.setExecutor(executor);
            server.createContext("/v1/anomalies/score-history", exchange->{
                try {
                    exchange.getRequestBody().readAllBytes();
                    if(delay>0) Thread.sleep(delay);
                    byte[] bytes=body.get().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","application/json");
                    exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
                } catch(InterruptedException exception) { Thread.currentThread().interrupt(); }
                  catch(java.io.IOException ignored) { /* AI작성: timeout으로 클라이언트가 끊은 연결은 테스트 서버에서만 무시한다. */ }
                finally { exchange.close(); }
            });server.start();
        }
        String url(){ return "http://127.0.0.1:"+server.getAddress().getPort(); }
        public void close(){ server.stop(0);executor.shutdownNow(); }
    }
    @Test void delayedResponseFailsWithinBound() throws Exception {
        try(var server=new Stub(valid(),2000)) {
            var service=new AnomalyServiceImpl(true,server.url(),500,100);
            assertTimeoutPreemptively(Duration.ofSeconds(3),()->{
                var error=assertThrows(ApiException.class,()->service.analyze(sample()));
                assertEquals("ANOMALY_CONNECTION_FAILED",error.getCode());
            });
        }
    }
    @Test void brokenJsonIsNotAcceptedAndDoesNotLeakBody() throws Exception {
        try(var server=new Stub("{private-secret-payload",0)) {
            var error=assertThrows(ApiException.class,()->new AnomalyServiceImpl(true,server.url(),500,1000).analyze(sample()));
            assertFalse(error.getMessage().contains("private-secret"));
        }
    }
    @Test void scoreAndReviewContradictionsRejected() throws Exception {
        try(var server=new Stub(valid(),0)) {
            var service=new AnomalyServiceImpl(true,server.url(),500,1000);
            for(String bad:List.of(valid().replace("\"is_anomaly\":true","\"is_anomaly\":false"),
                    valid().replace("\"requires_review\":false","\"requires_review\":true"),
                    valid().replace("\"anomaly_score\":0.7","\"anomaly_score\":null"))) {
                server.body.set(bad);
                assertEquals("ANOMALY_INVALID_RESPONSE",assertThrows(ApiException.class,()->service.analyze(sample())).getCode());
            }
        }
    }
    @Test void transactionContextAndQualityFlagsMustMatchContract() throws Exception {
        try(var server=new Stub(valid(),0)) {
            var service=new AnomalyServiceImpl(true,server.url(),500,1000);
            for(String bad:List.of(valid().replace("WITHDRAWAL","DEPOSIT"),
                    valid().replace("20:08:00","20:09:00"),
                    valid().replace("\"quality_flags\":[]","\"quality_flags\":[null]").replace("\"requires_review\":false","\"requires_review\":true"),
                    valid().replace("\"quality_flags\":[]","\"quality_flags\":[\"unknown_flag\"]").replace("\"requires_review\":false","\"requires_review\":true"))) {
                server.body.set(bad);
                assertEquals("ANOMALY_INVALID_RESPONSE",assertThrows(ApiException.class,()->service.analyze(sample())).getCode());
            }
        }
    }
    @Test void timezoneEquivalentResponseIsAccepted() throws Exception {
        try(var server=new Stub(valid().replace("2026-09-18T20:08:00+09:00","2026-09-18T11:08:00Z"),0)) {
            assertTrue(new AnomalyServiceImpl(true,server.url(),500,1000).analyze(sample()).is_anomaly());
        }
    }
}
