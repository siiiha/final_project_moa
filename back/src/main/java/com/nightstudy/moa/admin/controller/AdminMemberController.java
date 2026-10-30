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

    // AI작성: 관리자 회원 기능의 비즈니스 처리를 Service에 위임하기 위해 주입받는다.
    public AdminMemberController(AdminMemberService adminMemberService) {
        this.adminMemberService = adminMemberService;
    }

    @GetMapping
    // AI작성: 검색·상태·지역·페이징 조건을 검증하고 회원 목록과 페이지 정보를 반환한다.
    public ApiResponse<List<AdminMemberListItemResponse>> getMemberList(
            @Valid @ModelAttribute AdminMemberSearchCondition condition
    ) {
        AdminMemberListResult result = adminMemberService.getMemberList(condition);
        return ApiResponse.ok(result.getMembers(), result.getPageMeta());
    }

    @GetMapping("/{memberId}")
    // AI작성: 관리자 화면에 필요한 마스킹 회원 상세 정보와 활동 집계를 반환한다.
    public ApiResponse<AdminMemberDetailResponse> getMemberDetail(@PathVariable Long memberId) {
        return ApiResponse.ok(adminMemberService.getMemberDetail(memberId));
    }

    @PatchMapping("/{memberId}/restriction")
    // AI작성: 인증 필터가 전달한 관리자 번호와 검증된 요청값으로 회원 제한을 적용한다.
    public ApiResponse<Void> restrictMember(
            @RequestAttribute("memberId") Long administratorId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberRestrictionRequest request
    ) {
        adminMemberService.restrictMember(administratorId, memberId, request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{memberId}/restriction/release")
    // AI작성: 요청값으로 관리자 번호를 받지 않고 인증된 관리자 번호로 회원 제한을 해제한다.
    public ApiResponse<Void> releaseMemberRestriction(
            @RequestAttribute("memberId") Long administratorId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberRestrictionReleaseRequest request
    ) {
        adminMemberService.releaseMemberRestriction(administratorId, memberId, request);
        return ApiResponse.ok(null);
    }

    @InitBinder
    // AI작성: 공백만 입력한 검색어와 사유가 Validation을 우회하지 않도록 문자열을 정리한다.
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
