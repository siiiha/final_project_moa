package com.nightstudy.moa.smallGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.service.SmallGroupCreationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI작성: 소모임 생성 요청·응답만 처리하고 권한·생성 검증·저장은 Service에 위임한다.
@RestController
@RequestMapping("/api/smallgroups")
public class SmallGroupController {
    private final SmallGroupCreationService smallGroupCreationService;

    public SmallGroupController(SmallGroupCreationService smallGroupCreationService) {
        this.smallGroupCreationService = smallGroupCreationService;
    }

    // AI작성: 소모임 개설 화면은 로그인한 사용자만 DB에 등록된 활동 지역을 조회한다.
    @GetMapping("/regions")
    public ApiResponse<List<SmallGroupRegionResponse>> getSmallGroupRegions() {
        return ApiResponse.ok(smallGroupCreationService.getSmallGroupRegions());
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
