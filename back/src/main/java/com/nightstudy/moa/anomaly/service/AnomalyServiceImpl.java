package com.nightstudy.moa.anomaly.service;

import com.nightstudy.moa.anomaly.dto.AnomalyHistoryRequest;
import com.nightstudy.moa.anomaly.dto.AnomalyResult;
import com.nightstudy.moa.common.exception.ApiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// AI작성: 신뢰된 서버 설정의 Python 주소로만 호출한다. 기본 비활성화·재시도 없음·원문 로그 없음.
@Service
public class AnomalyServiceImpl implements AnomalyService {
    private final RestClient client;
    private final boolean enabled;
    // AI작성: Python 입력/출력 계약과 동일한 코드만 허용한다. 임의 문구·null은 저장 경로로 넘기지 않는다.
    private static final Set<String> QUALITY_FLAGS = Set.of(
            "bank_balance_missing", "source_sequence_missing", "same_timestamp_order_unknown",
            "ledger_balance_mismatch", "bank_balance_arithmetic_mismatch", "same_direction_history_under_5",
            "balance_before_nonpositive", "withdrawal_balance_unavailable", "withdrawal_exceeds_balance_before");

    public AnomalyServiceImpl(@Value("${moa.anomaly.enabled:false}") boolean enabled,
            @Value("${moa.anomaly.base-url:http://127.0.0.1:8001}") String baseUrl,
            @Value("${moa.anomaly.connect-timeout-ms:2000}") int connectTimeout,
            @Value("${moa.anomaly.read-timeout-ms:10000}") int readTimeout) {
        this.enabled = enabled;
        URI uri = URI.create(baseUrl);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                || connectTimeout < 1 || readTimeout < 1) {
            throw new IllegalArgumentException("이상감지 서버 주소 또는 제한시간 설정이 올바르지 않습니다.");
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeout))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public AnomalyResult analyze(AnomalyHistoryRequest request) {
        if (!enabled) throw failure(HttpStatus.SERVICE_UNAVAILABLE, "ANOMALY_DISABLED");
        if (request == null || request.account_id() == null || request.account_id().isBlank()
                || request.target_transaction_id() == null || request.target_transaction_id().isBlank()
                || request.transactions() == null || request.transactions().isEmpty()
                || request.transactions().size() > 2000) {
            throw failure(HttpStatus.BAD_REQUEST, "ANOMALY_INVALID_INPUT");
        }
        try {
            AnomalyResult result = client.post().uri("/v1/anomalies/score-history")
                    .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(AnomalyResult.class);
            // AI작성: 다른 거래 응답·미완료 응답·점수/경고 불일치를 저장하지 않도록 거부한다.
            if (result == null || !Objects.equals(request.account_id(), result.account_id())
                    || !Objects.equals(request.target_transaction_id(), result.transaction_id())
                    || !matchesTarget(request, result)
                    || result.anomaly_score() == null || !Double.isFinite(result.anomaly_score())
                    || result.threshold() == null || !Double.isFinite(result.threshold())
                    || result.is_anomaly() == null || result.is_anomaly() != (result.anomaly_score() > result.threshold())
                    || result.model_version() == null || result.model_version().isBlank()
                    || !"moa-anomaly-features-v1".equals(result.feature_schema_version())
                    || result.cold_start() == null || result.quality_flags() == null
                    || result.quality_flags().stream().anyMatch(flag -> flag == null || !QUALITY_FLAGS.contains(flag))
                    || result.requires_review() == null
                    || result.requires_review() != (result.cold_start() || !result.quality_flags().isEmpty())) {
                throw failure(HttpStatus.BAD_GATEWAY, "ANOMALY_INVALID_RESPONSE");
            }
            return result;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 422 || exception.getStatusCode().value() == 413)
                throw failure(HttpStatus.BAD_REQUEST, "ANOMALY_INVALID_INPUT");
            throw failure(HttpStatus.SERVICE_UNAVAILABLE, "ANOMALY_UPSTREAM_ERROR");
        } catch (RestClientException exception) {
            throw failure(HttpStatus.SERVICE_UNAVAILABLE, "ANOMALY_CONNECTION_FAILED");
        }
    }

    // AI작성: 같은 ID라도 종류/발생 시각이 다르면 거부한다. UTC/KST 표기가 달라도 같은 순간이면 허용한다.
    private boolean matchesTarget(AnomalyHistoryRequest request, AnomalyResult result) {
        var targets = request.transactions().stream().filter(transaction -> transaction != null
                && Objects.equals(transaction.transaction_id(), request.target_transaction_id())).toList();
        if (targets.size() != 1) return false;
        var target = targets.get(0);
        if (!Objects.equals(target.transaction_type(), result.transaction_type())
                || !Objects.equals(target.account_id(), result.account_id())
                || target.transacted_at() == null || result.transacted_at() == null) return false;
        try {
            return OffsetDateTime.parse(target.transacted_at()).isEqual(OffsetDateTime.parse(result.transacted_at()));
        } catch (java.time.format.DateTimeParseException exception) {
            return false;
        }
    }

    private ApiException failure(HttpStatus status, String code) {

        // AI작성: 외부 오류 본문·요청 거래·연결 주소를 사용자에게 전달하지 않는다.
        return new ApiException(status, code, "거래 이상감지 요청을 처리할 수 없습니다.");
    }
}
