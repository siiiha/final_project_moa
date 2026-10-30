package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/small-groups")
// AI작성: 관리자 소모임 조회는 기존 SMALLGROUP 공개 조회 로직을 재사용하고 관리자 경로만 분리한다.
public class AdminSmallGroupController {
    private final SmallGroupService smallGroupService;

    public AdminSmallGroupController(SmallGroupService smallGroupService) {
        this.smallGroupService = smallGroupService;
    }

    @GetMapping
    public ApiResponse<List<SmallGroupSummaryResponse>> getSmallGroupList(
            @Valid @ModelAttribute SmallGroupSearchCondition condition
    ) {
        SmallGroupListResponse result = smallGroupService.getSmallGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    @GetMapping("/{smallGroupId}")
    // AI작성: 관리자는 회원 관계에 따른 역할 정보 없이 기존 공개 상세 데이터만 조회한다.
    public ApiResponse<SmallGroupDetailResponse> getSmallGroupDetail(
            @PathVariable Long smallGroupId
    ) {
        return ApiResponse.ok(smallGroupService.getSmallGroupDetail(smallGroupId, null));
    }
}
