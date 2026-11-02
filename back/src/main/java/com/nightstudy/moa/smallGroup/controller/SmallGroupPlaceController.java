package com.nightstudy.moa.smallGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.SmallGroupPlaceService;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI작성: 기존 /api/** 로그인 정책을 따르고 회원·외부 연동 검증은 Service에 위임한다.
@RestController
@RequestMapping("/api/small-groups")
@RequiredArgsConstructor
public class SmallGroupPlaceController {
    private final SmallGroupPlaceService smallGroupPlaceService;

    // AI작성: 요청 DTO의 검색 조건만 바인딩하고 인증 회원 ID는 JWT 요청 속성에서만 읽는다.
    @InitBinder
    void bindSearchFields(WebDataBinder binder) {
        binder.setAllowedFields("smallGroupId", "query", "page", "size", "latitude", "longitude", "radius", "sort");
    }

    @GetMapping("/places")
    public ResponseEntity<ApiResponse<SmallGroupPlaceSearchResponse>> searchPlaces(
            @RequestAttribute("memberId") Long memberId,
            @Valid @ModelAttribute SmallGroupPlaceSearchRequest request) {
        var result = smallGroupPlaceService.searchPlaces(memberId, request);
        // AI작성: 위치 기준 검색 결과가 브라우저·공유 캐시에 저장되지 않도록 한다.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(result));
    }
}
