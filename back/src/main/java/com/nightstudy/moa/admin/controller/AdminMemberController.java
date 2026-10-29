package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListItemResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionReleaseRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionRequest;
import com.nightstudy.moa.admin.service.AdminMemberService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
// AI작성: SecurityConfig에서 ADMIN 권한을 확인한 회원 목록·상세 요청을 처리한다.
public class AdminMemberController {
    private final AdminMemberService adminMemberService;

    public AdminMemberController(AdminMemberService adminMemberService) {
        this.adminMemberService = adminMemberService;
    }

    @GetMapping
    public ApiResponse<List<AdminMemberListItemResponse>> getMemberList(
            @Valid @ModelAttribute AdminMemberSearchCondition condition
    ) {
        AdminMemberListResult result = adminMemberService.getMemberList(condition);
        return ApiResponse.ok(result.getMembers(), result.getPageMeta());
    }

    @GetMapping("/{memberId}")
    public ApiResponse<AdminMemberDetailResponse> getMemberDetail(@PathVariable Long memberId) {
        return ApiResponse.ok(adminMemberService.getMemberDetail(memberId));
    }

    @PatchMapping("/{memberId}/restriction")
    public ApiResponse<Void> restrictMember(
            @RequestAttribute("memberId") Long administratorId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberRestrictionRequest request
    ) {
        adminMemberService.restrictMember(administratorId, memberId, request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{memberId}/restriction/release")
    public ApiResponse<Void> releaseMemberRestriction(
            @RequestAttribute("memberId") Long administratorId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberRestrictionReleaseRequest request
    ) {
        adminMemberService.releaseMemberRestriction(administratorId, memberId, request);
        return ApiResponse.ok(null);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
