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
}
