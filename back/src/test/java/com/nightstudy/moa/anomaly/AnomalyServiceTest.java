package com.nightstudy.moa.anomaly;

import com.nightstudy.moa.anomaly.dto.AnomalyHistoryRequest;
import com.nightstudy.moa.anomaly.service.AnomalyServiceImpl;
import com.nightstudy.moa.common.exception.ApiException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// AI작성: Spring 전체 기동·DB 없이 실제 HTTP 직렬화/역직렬화와 실패 처리를 검증한다.
class AnomalyServiceTest {
    private AnomalyHistoryRequest sample() {
        return new AnomalyHistoryRequest("10013","2",List.of(
            new AnomalyHistoryRequest.Transaction("1","10013","WITHDRAWAL","50000.00","2026-09-18T20:00:00+09:00","1000000.00",1L),
            new AnomalyHistoryRequest.Transaction("2","10013","WITHDRAWAL","50000.00","2026-09-18T20:08:00+09:00","1000000.00",2L)));
    }
    private String response() {
        return """
            {"account_id":"10013","transaction_id":"2","transacted_at":"2026-09-18T20:08:00+09:00",
            "transaction_type":"WITHDRAWAL","anomaly_score":0.7,"threshold":0.6,"is_anomaly":true,
            "score_definition":"not a probability","model_version":"test-version",
            "feature_schema_version":"moa-anomaly-features-v1","cold_start":false,
            "quality_flags":[],"requires_review":false}
            """;
    }
    @Test void httpSuccessAndFailure() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicInteger status=new AtomicInteger(200);
        AtomicReference<String> responseBody=new AtomicReference<>(response());
        server.createContext("/v1/anomalies/score-history", exchange -> {
            String body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            assertTrue(body.contains("target_transaction_id"));
            byte[] bytes=(status.get()==200?responseBody.get():"{\"detail\":\"private upstream error\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status.get(),bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        String url="http://127.0.0.1:"+server.getAddress().getPort();
        try {
            var service=new AnomalyServiceImpl(true,url,500,1000);
            assertTrue(service.analyze(sample()).is_anomaly());
            responseBody.set(response().replace("\"transaction_id\":\"2\"", "\"transaction_id\":\"other\""));
            assertEquals("ANOMALY_INVALID_RESPONSE",assertThrows(ApiException.class,()->service.analyze(sample())).getCode());
            responseBody.set("{}");
            assertEquals("ANOMALY_INVALID_RESPONSE",assertThrows(ApiException.class,()->service.analyze(sample())).getCode());
            responseBody.set(response());
            status.set(422);
            assertEquals("ANOMALY_INVALID_INPUT",assertThrows(ApiException.class,()->service.analyze(sample())).getCode());
            status.set(503);
            assertEquals("ANOMALY_UPSTREAM_ERROR",assertThrows(ApiException.class,()->service.analyze(sample())).getCode());
            assertEquals("ANOMALY_DISABLED",assertThrows(ApiException.class,
                ()->new AnomalyServiceImpl(false,url,500,1000).analyze(sample())).getCode());
        } finally { server.stop(0); }
        assertEquals("ANOMALY_CONNECTION_FAILED",assertThrows(ApiException.class,
            ()->new AnomalyServiceImpl(true,url,500,1000).analyze(sample())).getCode());
    }
    @Test void realPythonHttp() {
        String url=System.getenv("MOA_TEST_PYTHON_URL");
        assumeTrue(url!=null && !url.isBlank(),"로컬 Python 서버 주소를 지정한 통합 검증에서만 실행");
        var result=new AnomalyServiceImpl(true,url,2000,10000).analyze(sample());
        assertEquals("2",result.transaction_id());
        assertEquals("sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",result.model_version());
        assertTrue(result.cold_start());
        assertEquals(result.anomaly_score()>result.threshold(),result.is_anomaly());
    }
}
