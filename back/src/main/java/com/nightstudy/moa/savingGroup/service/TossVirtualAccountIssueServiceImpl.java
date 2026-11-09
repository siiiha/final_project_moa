package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.config.TossVirtualAccountProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import java.net.http.HttpClient;
import java.time.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

// AI작성: 운영 키·임의 주소·자동 재시도를 허용하지 않는 테스트 전용 발급 계층이다.
@Service
public class TossVirtualAccountIssueServiceImpl implements VirtualAccountIssueService {
    // AI작성: 공식 은행 코드의 세 자리 응답을 설정에 사용하는 두 자리 코드로 정규화한다.
    // KB국민은행은 004 → 06이므로 앞의 0만 제거해서는 안 된다.
    private static final Map<String, String> BANK_CODES = Map.ofEntries(
            Map.entry("039", "39"), Map.entry("034", "34"), Map.entry("004", "06"),
            Map.entry("003", "03"), Map.entry("011", "11"), Map.entry("031", "31"),
            Map.entry("032", "32"), Map.entry("007", "07"), Map.entry("088", "88"),
            Map.entry("020", "20"), Map.entry("071", "71"), Map.entry("037", "37"), Map.entry("081", "81"));
    private final TossVirtualAccountProperties properties;
    private final RestClient restClient;
    public TossVirtualAccountIssueServiceImpl(TossVirtualAccountProperties properties) {
        this.properties = properties;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        restClient = RestClient.builder().baseUrl("https://api.tosspayments.com").requestFactory(factory).build();
    }
    @Override public void requireConfigured() {
        if (!properties.isEnabled() || properties.getSecretKey() == null
                || !properties.getSecretKey().matches("test_sk_[A-Za-z0-9]+")
                || properties.getBankCode() == null || !properties.getBankCode().matches("[0-9]{2}")
                || properties.getAccountHolderName() == null || properties.getAccountHolderName().isBlank()
                || properties.getAccountHolderName().length() > 100) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TOSS_TEST_NOT_CONFIGURED", "테스트 계좌 발급 설정을 확인해 주세요.");
        }
    }
    @Override public String accountHolderName() { requireConfigured(); return properties.getAccountHolderName(); }

    @Override public VirtualAccountIssueResult issue(VirtualAccountIssueRequest request) {
        requireConfigured();
        if (request == null || request.orderId() == null || !request.orderId().matches("moa_[A-Za-z0-9_-]{6,60}"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACCOUNT_REQUEST_INVALID", "발급 요청 정보가 올바르지 않습니다.");
        TossVirtualAccountResponse response;
        try {
            response = restClient.post().uri("/v1/virtual-accounts")
                    .headers(headers -> headers.setBasicAuth(properties.getSecretKey(), ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", 1000, "orderId", request.orderId(),
                            "orderName", "모아 계모임 테스트 계좌 발급", "customerName", "모아테스트",
                            "bank", properties.getBankCode(), "validHours", 168))
                    .retrieve().body(TossVirtualAccountResponse.class);
        } catch (RestClientResponseException e) {
            // AI작성: 인증 거절 외에는 중복 주문·처리 여부를 단정하지 않고 확인 필요 상태로 변환한다.
            String code = e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403
                    ? "ACCOUNT_ISSUE_REJECTED" : "ACCOUNT_ISSUE_UNKNOWN";
            throw new ApiException(HttpStatus.BAD_GATEWAY, code, "계좌 발급에 실패했습니다. 관리자 확인이 필요합니다.");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ACCOUNT_ISSUE_UNKNOWN", "계좌 발급 결과를 확인하지 못했습니다. 관리자 확인이 필요합니다.");
        }
        try {
            var account = response == null ? null : response.getVirtualAccount();
            if (account == null || !request.orderId().equals(response.getOrderId())
                    || !"WAITING_FOR_DEPOSIT".equals(response.getStatus()) || !"KRW".equals(response.getCurrency())
                    || !Integer.valueOf(1000).equals(response.getTotalAmount())
                    || !"일반".equals(account.getAccountType()) || !Boolean.FALSE.equals(account.getExpired())
                    || !properties.getBankCode().equals(normalizeBankCode(account.getBankCode())) || account.getAccountNumber() == null
                    || !account.getAccountNumber().matches("X[0-9]{4,19}")) throw new IllegalArgumentException();
            OffsetDateTime due;
            try { due = OffsetDateTime.parse(account.getDueDate()); }
            catch (java.time.format.DateTimeParseException e) {
                due = LocalDateTime.parse(account.getDueDate()).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
            }
            if (!due.isAfter(OffsetDateTime.now()) || due.isAfter(OffsetDateTime.now().plusDays(8))) throw new IllegalArgumentException();
            return new VirtualAccountIssueResult(properties.getBankCode(), account.getAccountNumber(), due);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ACCOUNT_RESPONSE_INVALID", "계좌 발급 응답을 확인할 수 없습니다. 관리자 확인이 필요합니다.");
        }
    }

    private String normalizeBankCode(String code) {
        return code != null && code.length() == 3 ? BANK_CODES.get(code) : code;
    }
}
