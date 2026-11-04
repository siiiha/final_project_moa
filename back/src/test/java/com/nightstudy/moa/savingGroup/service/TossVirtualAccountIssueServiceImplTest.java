package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.nightstudy.moa.common.config.TossVirtualAccountProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.VirtualAccountIssueRequest;
import java.time.OffsetDateTime;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// AI작성: HTTP 계층을 Mock 서버로 교체해 실제 토스 통신과 고정 비밀값을 사용하지 않는다.
class TossVirtualAccountIssueServiceImplTest {
    TossVirtualAccountProperties props;
    TossVirtualAccountIssueServiceImpl client;
    MockRestServiceServer server;
    final VirtualAccountIssueRequest request = new VirtualAccountIssueRequest(1L, 2L, "moa_test_order");
    @BeforeEach void setup() {
        props = new TossVirtualAccountProperties(); props.setEnabled(true);
        props.setSecretKey("test_sk_" + UUID.randomUUID().toString().replace("-", ""));
        props.setBankCode("00"); props.setAccountHolderName("모아테스트");
        client = new TossVirtualAccountIssueServiceImpl(props);
        var builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }
    String response() {
        return """
          {"orderId":"moa_test_order","status":"WAITING_FOR_DEPOSIT","currency":"KRW","totalAmount":1000,
          "virtualAccount":{"accountType":"일반","bankCode":"00","accountNumber":"X000000000000",
          "expired":false,"dueDate":"%s"}}
          """.formatted(OffsetDateTime.now().plusDays(7));
    }
    @Test void issuesTestAccountWithApprovedValues() {
        server.expect(requestTo("https://api.tosspayments.com/v1/virtual-accounts"))
                .andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.validHours").value(168)).andExpect(jsonPath("$.accountKey").doesNotExist())
                .andExpect(jsonPath("$.customerName").value("모아테스트"))
                .andRespond(withSuccess(response(), MediaType.APPLICATION_JSON));
        assertThat(client.issue(request).getAccountNumber()).startsWith("X"); server.verify();
    }
    @Test void missingOrLiveConfigurationMakesNoRequest() {
        props.setEnabled(false); assertThatThrownBy(() -> client.issue(request)).isInstanceOf(ApiException.class);
        props.setEnabled(true); props.setSecretKey("live_" + UUID.randomUUID());
        assertThatThrownBy(() -> client.issue(request)).isInstanceOf(ApiException.class); server.verify();
    }
    @ParameterizedTest @ValueSource(strings = {"null", "{}", "invalid-json"})
    void rejectsInvalidResponse(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.issue(request)).isInstanceOf(ApiException.class); server.verify();
    }
    @Test void rejectsMismatchedOrder() {
        server.expect(anything()).andRespond(withSuccess(response().replace("moa_test_order", "moa_other_order"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.issue(request)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCode()).isEqualTo("ACCOUNT_RESPONSE_INVALID"));
    }
    @Test void hidesErrorResponseAndTimeout() {
        server.expect(anything()).andRespond(withServerError().body("sensitive upstream response"));
        assertThatThrownBy(() -> client.issue(request)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getMessage()).doesNotContain("sensitive"));
        server.reset(); server.expect(anything()).andRespond(withException(new SocketTimeoutException("private detail")));
        assertThatThrownBy(() -> client.issue(request)).isInstanceOfSatisfying(ApiException.class,
                e -> { assertThat(e.getCode()).isEqualTo("ACCOUNT_ISSUE_UNKNOWN"); assertThat(e.getMessage()).doesNotContain("private"); });
    }

    // AI작성: KB국민은행의 공식 004 응답은 토스 두 자리 06 설정과 같은 은행이다.
    @Test void normalizesOfficialThreeDigitBankCode() {
        props.setBankCode("06");
        server.expect(anything()).andRespond(withSuccess(response().replace("\"bankCode\":\"00\"", "\"bankCode\":\"004\""), MediaType.APPLICATION_JSON));
        assertThat(client.issue(request).getBankCode()).isEqualTo("06"); server.verify();
    }

    // AI작성: 다른 은행·금액·통화·유형·만료 응답과 실계좌 형태가 일반 성공으로 저장되지 않는다.
    @ParameterizedTest
    @ValueSource(strings = {"bank", "amount", "currency", "type", "expired", "account", "dueDate"})
    void rejectsUnsafePaymentFields(String field) {
        String body = switch (field) {
            case "bank" -> response().replace("\"bankCode\":\"00\"", "\"bankCode\":\"88\"");
            case "amount" -> response().replace("1000", "2000");
            case "currency" -> response().replace("KRW", "USD");
            case "type" -> response().replace("일반", "고정");
            case "expired" -> response().replace("\"expired\":false", "\"expired\":true");
            case "account" -> response().replace("X000000000000", "000000000000");
            default -> response().replaceAll("\"dueDate\":\"[^\"]+\"", "\"dueDate\":\"2020-01-01T00:00:00+09:00\"");
        };
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.issue(request)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCode()).isEqualTo("ACCOUNT_RESPONSE_INVALID"));
        server.verify();
    }
}
