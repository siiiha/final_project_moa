package com.nightstudy.moa.smallGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCategoryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.SmallGroupCreationService;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

// AI작성: 소모임 생성·목록·상세 API를 같은 기본 경로에서 제공한다.
@RestController
@RequestMapping("/api/small-groups")
public class SmallGroupController {
    private final SmallGroupCreationService smallGroupCreationService;
    private final SmallGroupService smallGroupService;

    public SmallGroupController(
            SmallGroupCreationService smallGroupCreationService,
            SmallGroupService smallGroupService) {
        this.smallGroupCreationService = smallGroupCreationService;
        this.smallGroupService = smallGroupService;
    }

    // AI작성: 소모임 개설 화면은 로그인한 사용자의 활동 지역만 조회한다.
    @GetMapping("/regions")
    public ApiResponse<List<SmallGroupRegionResponse>> getSmallGroupRegions() {
        return ApiResponse.ok(smallGroupCreationService.getSmallGroupRegions());
    }

    // AI작성: 공개 목록의 관심사 필터는 DB 기준 데이터에서 조회한다.
    @GetMapping("/categories")
    public ApiResponse<List<SmallGroupCategoryResponse>> getSmallGroupCategories() {
        return ApiResponse.ok(smallGroupCreationService.getSmallGroupCategories());
    }

    // AI작성: 로그인 정보나 클라이언트 전달 회원 식별자 없이 공개 목록만 반환한다.
    @GetMapping
    public ApiResponse<List<SmallGroupSummaryResponse>> list(
            @Valid @ModelAttribute SmallGroupSearchCondition condition) {
        SmallGroupListResponse result = smallGroupService.getSmallGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    // AI작성: 공개 상세는 비로그인 요청도 허용하므로 memberId를 선택적으로 Service에 전달한다.
    @GetMapping("/{id}")
    public ApiResponse<SmallGroupDetailResponse> detail(
            @PathVariable("id") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long id,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(smallGroupService.getSmallGroupDetail(id, memberId));
    }

    // AI작성: 설정 정보는 JWT 필터가 검증한 회원 ID를 Service에만 전달하고 권한 판단은 Service에서 처리한다.
    @GetMapping("/{id}/settings")
    public ApiResponse<SmallGroupSettingsResponse> getSettings(
            @PathVariable("id") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long id,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(smallGroupService.getSmallGroupSettings(id, memberId));
    }

    // AI작성: 수정 권한과 정원 축소 정책은 Service가 JWT 인증 회원 ID를 기준으로 검증한다.
    @PatchMapping("/{id}")
    public ApiResponse<Void> updateSettings(
            @PathVariable("id") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long id,
            @Valid @RequestBody SmallGroupSettingsPatchRequest request,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        smallGroupService.updateSmallGroupSettings(id, memberId, request);
        return ApiResponse.ok(null);
    }

    @PostMapping
    public ApiResponse<SmallGroupCreationResponse> createSmallGroup(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody SmallGroupCreationRequest request) {
        // AI작성: JWT 필터가 검증해 저장한 회원 ID만 생성자 정보로 사용한다.
        request.setMemberId(memberId);
        return ApiResponse.ok(smallGroupCreationService.createSmallGroup(request));
    }
}
