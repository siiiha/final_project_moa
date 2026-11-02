package com.nightstudy.moa.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.nightstudy.moa.common.config.KakaoOAuthProperties;
import com.nightstudy.moa.common.exception.ApiException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// AI작성: 기존 테스트 도구의 Mock 서버를 기존 RestClient 필드에 연결해 실제 외부 통신을 차단한다.
class KakaoPlaceApiClientTest {
    private KakaoApiClient client;
    private MockRestServiceServer server;
    private static final String EMPTY = """
            {"meta":{"total_count":0,"pageable_count":0,"is_end":true},"documents":[]}
            """;

    @BeforeEach
    void setUp() {
        KakaoOAuthProperties properties = new KakaoOAuthProperties();
        properties.setClientId("mock-rest-key");
        client = new KakaoApiClient(properties);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @Test
    void sendsEncodedQueryWithExistingAuthenticationAndNoImplicitLocation() {
        server.expect(requestTo("https://dapi.kakao.com/v2/local/search/keyword.json?query=A%26B%20%2B%20%23&page=1&size=15&sort=accuracy"))
                .andExpect(header("Authorization", "KakaoAK mock-rest-key"))
                .andRespond(withSuccess(EMPTY, MediaType.APPLICATION_JSON));
        assertThat(client.searchPlaces("A&B + #", 1, 15, null, null, null, "accuracy").getDocuments()).isEmpty();
        server.verify();
    }

    @Test
    void sendsLocationRadiusAndDistanceSortAndReadsRequiredFields() {
        server.expect(queryParam("x", "0.0")).andExpect(queryParam("y", "0.0"))
                .andExpect(queryParam("radius", "1000")).andExpect(queryParam("sort", "distance"))
                .andExpect(queryParam("page", "2")).andExpect(queryParam("size", "5"))
                .andRespond(withSuccess("""
                        {"meta":{"total_count":1,"pageable_count":1,"is_end":true},
                         "documents":[{"id":"test-1","place_name":"가상 장소","x":"0","y":"0",
                         "address_name":"가상 주소","road_address_name":"가상 도로","category_name":"가상 분류",
                         "place_url":"https://place.map.kakao.com/1","distance":"100","unknown":"ignored"}]}
                        """, MediaType.APPLICATION_JSON));
        var response = client.searchPlaces("가상", 2, 5, 0.0, 0.0, 1000, "distance");
        assertThat(response.getDocuments().get(0).getPlaceName()).isEqualTo("가상 장소");
        assertThat(response.getDocuments().get(0).getLatitude()).isEqualTo("0");
        assertThat(response.getMeta().getEnd()).isTrue();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"documents\":[]}", "{\"meta\":{}}", "null", "invalid-json",
            "{\"meta\":{\"total_count\":-1,\"pageable_count\":0,\"is_end\":true},\"documents\":[]}"})
    void rejectsMalformedExternalResponses(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.searchPlaces("가상", 1, 15, null, null, null, "accuracy"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getMessage()).doesNotContain(body);
                });
        server.verify();
    }

    @Test
    void hidesExternalErrorBodyAndConnectionFailure() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("private-upstream-data"));
        server.expect(anything()).andRespond(withException(new IOException("private-location")));
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> client.searchPlaces("가상", 1, 15, null, null, null, "accuracy"))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.getCode()).isEqualTo("KAKAO_PLACE_REQUEST_FAILED");
                        assertThat(e.getMessage()).doesNotContain("private", "mock-rest-key");
                        assertThat(e.getCause()).isNull();
                    });
        }
        server.verify();
    }
}
