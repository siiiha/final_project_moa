package com.nightstudy.moa.smallGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.SmallGroupCreationService;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI작성: 기존 소모임 생성 경로에 목록 조회를 추가하고 생성 기능은 유지한다.
@RestController
@RequestMapping("/api/smallgroups")
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

    // AI작성: 로그인 정보나 클라이언트 전달 회원 식별자 없이 공개 목록만 반환한다.
    @GetMapping
    public ApiResponse<List<SmallGroupSummaryResponse>> list() {
        return ApiResponse.ok(smallGroupService.getSmallGroupList());
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
