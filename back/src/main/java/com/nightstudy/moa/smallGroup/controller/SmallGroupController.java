package com.nightstudy.moa.smallGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.service.SmallGroupCreationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    public ApiResponse<SmallGroupCreationResponse> createSmallGroup(
            @Valid @RequestBody SmallGroupCreationRequest request) {
        // AI작성: TODO: JWT 연동 후 요청 본문의 임시 memberId를 제거하고 토큰의 로그인 회원 식별값으로 처리한다.
        return ApiResponse.ok(smallGroupCreationService.createSmallGroup(request));
    }
}
