package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.social.kakao.KakaoPlaceResponse;
import com.nightstudy.moa.member.service.social.kakao.KakaoApiClient;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// AI작성: 기존 상세·회원 관계 Mapper와 카카오 Client를 재사용하고 검색 결과나 위치를 저장하지 않는다.
@Service
@RequiredArgsConstructor
public class SmallGroupPlaceServiceImpl implements SmallGroupPlaceService {
    private final SmallGroupMapper smallGroupMapper;
    private final KakaoApiClient kakaoApiClient;
    private final Validator validator;

    @Override
    public SmallGroupPlaceSearchResponse searchPlaces(Long memberId, SmallGroupPlaceSearchRequest request) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
        }
        // AI작성: Controller 이외의 호출에서도 검증하며 오류에 입력한 위치나 검색어를 포함하지 않는다.
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "장소 검색 조건을 확인해 주세요.");
        }
        var target = smallGroupMapper.selectSmallGroupDetail(request.getSmallGroupId());
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        var relations = smallGroupMapper.selectSmallGroupMemberRelations(target.getGroupId(), memberId);
        // AI작성: 기존 투표 접근 정책처럼 단일 APPROVED OWNER·MEMBER 관계만 허용한다. 중복 관계도 차단한다.
        if (relations == null || relations.size() != 1 || relations.get(0) == null
                || !"APPROVED".equals(relations.get(0).getMembershipStatus())
                || !("OWNER".equals(relations.get(0).getMemberRole())
                || "MEMBER".equals(relations.get(0).getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_PLACE_FORBIDDEN",
                    "승인된 소모임 회원만 장소를 검색할 수 있습니다.");
        }

        // AI작성: 외부 응답 대기 동안 DB 트랜잭션과 행 잠금을 유지하지 않는다.
        KakaoPlaceResponse response = kakaoApiClient.searchPlaces(request.getQuery(), request.getPage(),
                request.getSize(), request.getLatitude(), request.getLongitude(), request.getRadius(), request.getSort());
        if (response.getDocuments().isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_PLACE_NOT_FOUND",
                    "검색 결과가 없습니다. 검색어나 검색 조건을 바꿔 주세요.");
        }
        List<SmallGroupPlaceSearchResponse.Place> items = response.getDocuments().stream()
                .map(place -> toPlace(place, request.getLatitude() != null)).toList();
        return new SmallGroupPlaceSearchResponse(items, request.getPage(), request.getSize(),
                response.getMeta().getTotalCount(), response.getMeta().getPageableCount(), response.getMeta().getEnd());
    }

    // AI작성: 잘못된 외부 좌표·링크·거리 값은 원문을 노출하지 않고 기존 ApiException 형식으로 변환한다.
    private SmallGroupPlaceSearchResponse.Place toPlace(KakaoPlaceResponse.Place place, boolean hasPosition) {
        try {
            if (place == null || place.getId() == null || place.getId().isBlank()
                    || place.getPlaceName() == null || place.getPlaceName().isBlank()) {
                throw new IllegalArgumentException();
            }
            Double latitude = coordinate(place.getLatitude(), 90);
            Double longitude = coordinate(place.getLongitude(), 180);
            Integer distance = null;
            if (hasPosition && place.getDistance() != null && !place.getDistance().isBlank()) {
                distance = Integer.valueOf(place.getDistance());
                if (distance < 0) throw new IllegalArgumentException();
            }
            URI link = URI.create(place.getPlaceUrl());
            if (!("http".equals(link.getScheme()) || "https".equals(link.getScheme()))
                    || !"place.map.kakao.com".equals(link.getHost()) || link.getUserInfo() != null
                    || link.getPort() != -1 || !link.getPath().matches("/[0-9]+")
                    || link.getQuery() != null || link.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return new SmallGroupPlaceSearchResponse.Place(place.getId(), place.getPlaceName(),
                    place.getRoadAddress(), place.getAddress(), place.getCategory(), latitude, longitude,
                    "https://place.map.kakao.com" + link.getPath(), distance);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_PLACE_RESPONSE_INVALID",
                    "장소 검색 응답을 확인할 수 없습니다. 다시 시도해 주세요.");
        }
    }

    private Double coordinate(String value, int maximum) {
        double coordinate = Double.parseDouble(value);
        if (!Double.isFinite(coordinate) || coordinate < -maximum || coordinate > maximum) {
            throw new IllegalArgumentException();
        }
        return coordinate;
    }
}
