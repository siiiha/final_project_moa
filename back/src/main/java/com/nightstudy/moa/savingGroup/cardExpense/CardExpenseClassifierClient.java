package com.nightstudy.moa.savingGroup.cardExpense;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nightstudy.moa.common.exception.ApiException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI작성: Spring 서버는 권한·업로드·저장을 담당하고, 학습 모델 예측만 내부 Python API에 요청한다.
 */
@Component
public class CardExpenseClassifierClient {
    private final RestClient restClient;

    public CardExpenseClassifierClient(
            @Value("${moa.ml.classifier-url:http://127.0.0.1:8011}") String classifierUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(classifierUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public ClassificationResult classify(String merchantName, BigDecimal amount) {
        if (merchantName == null || merchantName.isBlank() || amount == null || amount.signum() < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CARD_EXPENSE_CLASSIFICATION_INPUT_INVALID",
                    "가맹점명과 이용금액을 확인해 주세요."
            );
        }

        PythonClassificationResponse response;
        try {
            response = restClient.post()
                    .uri("/classifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new ClassificationRequest(merchantName.trim(), amount))
                    .retrieve()
                    .body(PythonClassificationResponse.class);
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CARD_EXPENSE_CLASSIFIER_UNAVAILABLE",
                    "자동 분류 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
            );
        }

        if (response == null || response.reviewStatus() == null || response.predictionSource() == null) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "CARD_EXPENSE_CLASSIFIER_RESPONSE_INVALID",
                    "자동 분류 결과를 확인할 수 없습니다."
            );
        }

        return new ClassificationResult(
                toCategory(response.suggestedCategory()),
                response.confidence(),
                toReviewStatus(response.reviewStatus()),
                toClassificationSource(response.predictionSource()),
                response.reviewReason()
        );
    }

    private CardExpenseCategory toCategory(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (CardExpenseCategory category : CardExpenseCategory.values()) {
            if (category.getDisplayName().equals(displayName)) {
                return category;
            }
        }
        throw invalidResponse();
    }

    private CardExpenseClassificationStatus toReviewStatus(String value) {
        return switch (value) {
            case "검토 대기" -> CardExpenseClassificationStatus.REVIEW_PENDING;
            case "확인 필요" -> CardExpenseClassificationStatus.REVIEW_REQUIRED;
            default -> throw invalidResponse();
        };
    }

    private CardExpenseClassificationSource toClassificationSource(String value) {
        return switch (value) {
            case "keyword_rule" -> CardExpenseClassificationSource.KEYWORD_RULE;
            case "ml_model" -> CardExpenseClassificationSource.MACHINE_LEARNING;
            case "payment_intermediary_rule" -> CardExpenseClassificationSource.PAYMENT_PROVIDER;
            default -> throw invalidResponse();
        };
    }

    private ApiException invalidResponse() {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "CARD_EXPENSE_CLASSIFIER_RESPONSE_INVALID",
                "자동 분류 결과를 확인할 수 없습니다."
        );
    }

    private record ClassificationRequest(
            @JsonProperty("merchant_name") String merchantName,
            BigDecimal amount
    ) {
    }

    private record PythonClassificationResponse(
            @JsonProperty("suggested_category") String suggestedCategory,
            Double confidence,
            @JsonProperty("review_status") String reviewStatus,
            @JsonProperty("review_reason") String reviewReason,
            @JsonProperty("prediction_source") String predictionSource
    ) {
    }

    public record ClassificationResult(
            CardExpenseCategory suggestedCategory,
            Double confidence,
            CardExpenseClassificationStatus classificationStatus,
            CardExpenseClassificationSource classificationSource,
            String reviewReason
    ) {
    }
}
