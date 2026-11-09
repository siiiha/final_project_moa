package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.smallGroup.controller.SmallGroupPlaceController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchResponse;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// AI작성: 기존 보안 필터와 예외 처리기를 포함해 실제 DB·외부 호출 없는 API 계약을 검증한다.
@WebMvcTest(SmallGroupPlaceController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class, SmallGroupPlaceControllerTest.SecurityBootstrap.class})
class SmallGroupPlaceControllerTest {
    @TestConfiguration
    @EnableWebSecurity
    static class SecurityBootstrap { }

    @Autowired private MockMvc mvc;
    @MockitoBean private SmallGroupPlaceService service;
    @MockitoBean private JwtTokenProvider tokenProvider;
    @MockitoBean private AuthSessionService authSessionService;

    private void login() {
        var claims = Jwts.claims().subject("30").id("mock-id")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .add("role", "USER").add("sid", "mock-session").build();
        when(tokenProvider.parseToken("mock-access")).thenReturn(claims);
        when(authSessionService.isAccessAllowed(eq(30L), eq("USER"), eq("mock-session"), eq("mock-id"), anyLong()))
                .thenReturn(true);
    }

    @Test
    void anonymousCannotBypassAuthenticationWithMemberAttribute() throws Exception {
        mvc.perform(get("/api/small-groups/places").param("smallGroupId", "10").param("query", "가상")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        verifyNoInteractions(service);
    }

    @Test
    void bindsDefaultsAndUsesJwtIdentityInsteadOfQueryMemberId() throws Exception {
        login();
        when(service.searchPlaces(eq(30L), any())).thenAnswer(invocation -> {
            SmallGroupPlaceSearchRequest request = invocation.getArgument(1);
            assertThat(request.getSmallGroupId()).isEqualTo(10L);
            assertThat(request.getQuery()).isEqualTo("가상");
            assertThat(request.getPage()).isEqualTo(1);
            assertThat(request.getSize()).isEqualTo(15);
            assertThat(request.getSort()).isEqualTo("accuracy");
            assertThat(request.getLatitude()).isNull();
            return result();
        });
        mvc.perform(get("/api/small-groups/places").header("Authorization", "Bearer mock-access")
                        .param("smallGroupId", "10").param("query", "  가상  ").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].placeName").value("가상 장소"))
                .andExpect(jsonPath("$.data.items[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.latitude").doesNotExist())
                .andExpect(jsonPath("$.data.end").value(true));
    }

    @Test
    void bindsLocationRadiusSortAndPagination() throws Exception {
        login();
        when(service.searchPlaces(eq(30L), any())).thenAnswer(invocation -> {
            SmallGroupPlaceSearchRequest request = invocation.getArgument(1);
            assertThat(request.getLatitude()).isZero();
            assertThat(request.getLongitude()).isZero();
            assertThat(request.getRadius()).isEqualTo(1000);
            assertThat(request.getSort()).isEqualTo("distance");
            assertThat(request.getPage()).isEqualTo(2);
            assertThat(request.getSize()).isEqualTo(5);
            return result();
        });
        mvc.perform(get("/api/small-groups/places").header("Authorization", "Bearer mock-access")
                        .param("smallGroupId", "10").param("query", "가상")
                        .param("latitude", "0").param("longitude", "0").param("radius", "1000")
                        .param("sort", "distance").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"query,' '", "query,''", "page,0", "page,46", "page,abc", "size,0", "size,16",
            "smallGroupId,0", "latitude,NaN", "latitude,0", "longitude,0", "radius,1000", "sort,distance", "sort,unknown"})
    void rejectsInvalidRequestsBeforeService(String name, String value) throws Exception {
        login();
        var request = get("/api/small-groups/places").header("Authorization", "Bearer mock-access");
        if (!"smallGroupId".equals(name)) request.param("smallGroupId", "10");
        if (!"query".equals(name)) request.param("query", "가상");
        mvc.perform(request.param(name, value)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingGroupAndQuery() throws Exception {
        login();
        mvc.perform(get("/api/small-groups/places").header("Authorization", "Bearer mock-access"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"403,SMALL_GROUP_PLACE_FORBIDDEN", "404,SMALL_GROUP_NOT_FOUND",
            "404,SMALL_GROUP_PLACE_NOT_FOUND", "502,KAKAO_PLACE_REQUEST_FAILED", "502,KAKAO_PLACE_RESPONSE_INVALID"})
    void preservesServiceFailureEnvelope(int status, String code) throws Exception {
        login();
        when(service.searchPlaces(eq(30L), any()))
                .thenThrow(new ApiException(HttpStatus.valueOf(status), code, "조회할 수 없습니다."));
        mvc.perform(get("/api/small-groups/places").header("Authorization", "Bearer mock-access")
                        .param("smallGroupId", "10").param("query", "가상"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(code));
    }

    @Test
    void invalidSessionCannotSearch() throws Exception {
        login();
        when(authSessionService.isAccessAllowed(anyLong(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(false);
        mvc.perform(get("/api/small-groups/places").header("Authorization", "Bearer mock-access")
                        .param("smallGroupId", "10").param("query", "가상"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        verifyNoInteractions(service);
    }

    private SmallGroupPlaceSearchResponse result() {
        return new SmallGroupPlaceSearchResponse(List.of(new SmallGroupPlaceSearchResponse.Place(
                "test-1", "가상 장소", "가상 도로", "가상 주소", "가상 분류", 0.0, 0.0,
                "https://place.map.kakao.com/1", null)), 1, 15, 1, 1, true);
    }
}
