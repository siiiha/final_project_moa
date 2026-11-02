package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.social.kakao.KakaoPlaceResponse;
import com.nightstudy.moa.member.service.KakaoApiClient;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchRequest;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

// AI작성: DB·외부 호출 없이 승인 회원 접근과 외부 결과 변환·실패를 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupPlaceServiceTest {
    @Mock private SmallGroupMapper mapper;
    @Mock private KakaoApiClient client;
    private static ValidatorFactory factory;
    private SmallGroupPlaceServiceImpl service;
    private SmallGroupPlaceSearchRequest request;

    @BeforeAll
    static void createValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll
    static void closeValidator() { factory.close(); }

    @BeforeEach
    void setUp() {
        service = new SmallGroupPlaceServiceImpl(mapper, client, factory.getValidator());
        request = new SmallGroupPlaceSearchRequest();
        request.setSmallGroupId(10L);
        request.setQuery("가상 장소");
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "MEMBER"})
    void permitsApprovedMembersAndMapsOnlyPlaceInformation(String role) {
        allow(role, "APPROVED");
        mockResponse(response());
        var result = service.searchPlaces(30L, request);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(15);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.isEnd()).isTrue();
        var place = result.getItems().get(0);
        assertThat(place.getPlaceName()).isEqualTo("가상 장소");
        assertThat(place.getLatitude()).isZero();
        assertThat(place.getLongitude()).isZero();
        assertThat(place.getPlaceUrl()).isEqualTo("https://place.map.kakao.com/1");
        assertThat(place.getDistanceMeters()).isNull();
        verify(mapper).selectSmallGroupDetail(10L);
        verify(mapper).selectSmallGroupMemberRelations(20L, 30L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void passesLocationAndRadiusWithoutCalculatingDistance() {
        allow("MEMBER", "APPROVED");
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        request.setRadius(1000);
        request.setSort("distance");
        request.setPage(2);
        request.setSize(5);
        when(client.searchPlaces("가상 장소", 2, 5, 0.0, 0.0, 1000, "distance")).thenReturn(response());
        assertThat(service.searchPlaces(30L, request).getItems().get(0).getDistanceMeters()).isEqualTo(42);
    }

    @Test
    void rejectsMissingLoginBeforeDatabaseAndExternalLookup() {
        assertError(null, request, "LOGIN_REQUIRED", HttpStatus.UNAUTHORIZED);
        assertError(0L, request, "LOGIN_REQUIRED", HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(mapper, client);
    }

    @Test
    void validatesRequestsBeforeLookup() {
        assertError(30L, null, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
        request.setQuery(" ");
        assertError(30L, request, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
        request.setQuery("가상");
        request.setSort("distance");
        assertError(30L, request, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(mapper, client);
    }

    @Test
    void rejectsMissingOrDeletedGroupWithoutCallingClient() {
        assertError(30L, request, "SMALL_GROUP_NOT_FOUND", HttpStatus.NOT_FOUND);
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @CsvSource({"MEMBER,PENDING", "MEMBER,REJECTED", "MEMBER,LEFT", "MEMBER,KICKED",
            "OWNER,PENDING", "MANAGER,APPROVED", "OTHER,APPROVED"})
    void rejectsUnapprovedAndUnsupportedRelations(String role, String status) {
        allow(role, status);
        assertError(30L, request, "SMALL_GROUP_PLACE_FORBIDDEN", HttpStatus.FORBIDDEN);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsMissingAndDuplicateMemberships() {
        target();
        when(mapper.selectSmallGroupMemberRelations(20L, 30L)).thenReturn(List.of());
        assertError(30L, request, "SMALL_GROUP_PLACE_FORBIDDEN", HttpStatus.FORBIDDEN);
        var relation = relation("MEMBER", "APPROVED");
        when(mapper.selectSmallGroupMemberRelations(20L, 30L)).thenReturn(List.of(relation, relation));
        assertError(30L, request, "SMALL_GROUP_PLACE_FORBIDDEN", HttpStatus.FORBIDDEN);
        verifyNoInteractions(client);
    }

    @Test
    void treatsEmptySearchAsError() {
        allow("MEMBER", "APPROVED");
        var response = response();
        response.setDocuments(List.of());
        mockResponse(response);
        assertError(30L, request, "SMALL_GROUP_PLACE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @Test
    void preservesExternalFailureWithoutRetryOrWrites() {
        allow("MEMBER", "APPROVED");
        var failure = new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PLACE_REQUEST_FAILED", "외부 조회 실패");
        when(client.searchPlaces("가상 장소", 1, 15, null, null, null, "accuracy")).thenThrow(failure);
        assertThatThrownBy(() -> service.searchPlaces(30L, request)).isSameAs(failure);
        verify(client, times(1)).searchPlaces("가상 장소", 1, 15, null, null, null, "accuracy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "91", "invalid"})
    void rejectsInvalidExternalCoordinates(String latitude) {
        allow("MEMBER", "APPROVED");
        var response = response();
        response.getDocuments().get(0).setLatitude(latitude);
        mockResponse(response);
        assertError(30L, request, "KAKAO_PLACE_RESPONSE_INVALID", HttpStatus.BAD_GATEWAY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "https://example.invalid/1", "https://place.map.kakao.com/1?private=data"})
    void rejectsUnsafeExternalLinks(String link) {
        allow("MEMBER", "APPROVED");
        var response = response();
        response.getDocuments().get(0).setPlaceUrl(link);
        mockResponse(response);
        assertError(30L, request, "KAKAO_PLACE_RESPONSE_INVALID", HttpStatus.BAD_GATEWAY);
    }

    @Test
    void rejectsInvalidExternalDistance() {
        allow("MEMBER", "APPROVED");
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        var response = response();
        response.getDocuments().get(0).setDistance("-1");
        when(client.searchPlaces("가상 장소", 1, 15, 0.0, 0.0, null, "accuracy")).thenReturn(response);
        assertError(30L, request, "KAKAO_PLACE_RESPONSE_INVALID", HttpStatus.BAD_GATEWAY);
    }

    private void assertError(Long memberId, SmallGroupPlaceSearchRequest input, String code, HttpStatus status) {
        assertThatThrownBy(() -> service.searchPlaces(memberId, input)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getCode()).isEqualTo(code);
            assertThat(e.getStatus()).isEqualTo(status);
        });
    }

    private void target() {
        SmallGroupDetailRow target = new SmallGroupDetailRow();
        target.setSmallGroupId(10L);
        target.setGroupId(20L);
        when(mapper.selectSmallGroupDetail(10L)).thenReturn(target);
    }

    private SmallGroupMemberRelationRow relation(String role, String status) {
        var relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        return relation;
    }

    private void allow(String role, String status) {
        target();
        when(mapper.selectSmallGroupMemberRelations(20L, 30L)).thenReturn(List.of(relation(role, status)));
    }

    private void mockResponse(KakaoPlaceResponse response) {
        when(client.searchPlaces("가상 장소", 1, 15, null, null, null, "accuracy")).thenReturn(response);
    }

    private KakaoPlaceResponse response() {
        var response = new KakaoPlaceResponse();
        var meta = new KakaoPlaceResponse.Meta();
        meta.setTotalCount(1);
        meta.setPageableCount(1);
        meta.setEnd(true);
        response.setMeta(meta);
        var place = new KakaoPlaceResponse.Place();
        place.setId("test-1");
        place.setPlaceName("가상 장소");
        place.setLatitude("0");
        place.setLongitude("0");
        place.setDistance("42");
        place.setPlaceUrl("http://place.map.kakao.com/1");
        response.setDocuments(List.of(place));
        return response;
    }
}
