package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminGroupRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminSmallGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSmallGroupListResult;
import com.nightstudy.moa.admin.service.AdminSmallGroupService;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/small-groups")
// AI작성: 관리자 소모임 조회는 기존 SMALLGROUP 공개 조회 로직을 재사용하고 관리자 경로만 분리한다.
public class AdminSmallGroupController {
    private final SmallGroupService smallGroupService;
    private final AdminSmallGroupService adminSmallGroupService;

    public AdminSmallGroupController(
            SmallGroupService smallGroupService,
            AdminSmallGroupService adminSmallGroupService
    ) {
        this.smallGroupService = smallGroupService;
        this.adminSmallGroupService = adminSmallGroupService;
    }

    @GetMapping
    public ApiResponse<List<AdminSmallGroupListItemResponse>> getSmallGroupList(
            @Valid @ModelAttribute SmallGroupSearchCondition condition
    ) {
        AdminSmallGroupListResult result = adminSmallGroupService.getSmallGroupList(condition);
        return ApiResponse.ok(result.getSmallGroups(), result.getPageMeta());
    }

    @GetMapping("/{smallGroupId}")
    // AI작성: 관리자는 회원 관계에 따른 역할 정보 없이 기존 공개 상세 데이터만 조회한다.
    public ApiResponse<SmallGroupDetailResponse> getSmallGroupDetail(
            @PathVariable Long smallGroupId
    ) {
        return ApiResponse.ok(smallGroupService.getSmallGroupDetail(smallGroupId, null));
    }

    @PatchMapping("/{smallGroupId}/restriction")
    // AI작성: 요청 검증 후 소모임 운영정지 처리를 관리자 Service에 위임한다.
    public ApiResponse<Void> restrictSmallGroup(
            @PathVariable Long smallGroupId,
            @Valid @RequestBody AdminGroupRestrictionRequest request
    ) {
        adminSmallGroupService.restrictSmallGroup(smallGroupId, request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{smallGroupId}/restriction/release")
    // AI작성: 요청 검증 후 관리자 운영정지 해제 처리를 Service에 위임한다.
    public ApiResponse<Void> releaseSmallGroupRestriction(
            @PathVariable Long smallGroupId,
            @Valid @RequestBody AdminGroupRestrictionRequest request
    ) {
        adminSmallGroupService.releaseSmallGroupRestriction(smallGroupId, request);
        return ApiResponse.ok(null);
    }
}
