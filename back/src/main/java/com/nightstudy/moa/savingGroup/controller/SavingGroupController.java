package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSummaryResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestListResponse;
import jakarta.validation.constraints.Positive;
import java.util.List;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDeletionRequest;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// AI작성: /api 경로는 기존 프론트 클라이언트와 개발 프록시를 위한 동일 API 별칭이다.
@RestController
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class SavingGroupController {
    private final SavingGroupService savingGroupService;

    // AI작성: 목록 조회 서비스만 주입한다.
    public SavingGroupController(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    // AI작성: JWT 필터가 설정한 인증 회원 ID가 없으면 신청 목록을 조회하지 않는다.
    @GetMapping("/{savingGroupId}/join-requests")
    public ApiResponse<List<SavingGroupJoinRequestRow>> joinRequests(
            @PathVariable("savingGroupId") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        SavingGroupJoinRequestListResponse result = savingGroupService.getSavingGroupJoinRequests(savingGroupId, memberId, status, page);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    // AI작성: 가입 신청자는 요청값이 아닌 JWT 필터가 검증한 회원 ID로만 식별한다.
    @PostMapping("/{savingGroupId}/join-requests")
    public ApiResponse<Void> requestJoin(
            @PathVariable("savingGroupId") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.requestSavingGroupJoin(savingGroupId, memberId);
        return ApiResponse.ok();
    }

    @PostMapping("/{savingGroupId}/deletion-applications")
    // AI작성: 삭제 신청자는 요청 본문이 아닌 JWT 회원 번호로 계주 권한을 확인한다.
    public ApiResponse<Void> requestDeletion(
            @PathVariable Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SavingGroupDeletionRequest request
    ) {
        savingGroupService.requestSavingGroupDeletion(savingGroupId, memberId, request.getReason());
        return ApiResponse.ok();
    }

    // AI작성: 승인자는 JWT 필터가 검증한 서버 인증 정보로만 식별하며 요청 본문은 사용하지 않는다.
    @PatchMapping("/{savingGroupId}/join-requests/{crewMemberId}/approve")
    public ApiResponse<Void> approveJoinRequest(
            @PathVariable("savingGroupId") Long savingGroupId,
            @PathVariable("crewMemberId") Long crewMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.approveSavingGroupJoin(savingGroupId, crewMemberId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 거절도 승인과 같은 JWT 인증 회원 ID만 사용하며 요청 본문·쿼리로 처리자를 받지 않는다.
    @PatchMapping("/{savingGroupId}/join-requests/{crewMemberId}/reject")
    public ApiResponse<Void> rejectJoinRequest(
            @PathVariable("savingGroupId") Long savingGroupId,
            @PathVariable("crewMemberId") Long crewMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.rejectSavingGroupJoin(savingGroupId, crewMemberId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: JWT 필터가 검증해 요청에 저장한 회원 ID만 상세 역할 조회에 사용한다.
    @GetMapping("/{id}")
    public ApiResponse<SavingGroupDetailResponse> detail(
            @PathVariable("id") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long id,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(savingGroupService.getSavingGroupDetail(id, memberId));
    }

    // AI작성: 인증 정보 없이 공개 목록을 조회하고 기존 성공 응답으로 감싼다.
    // AI작성: 게시글과 동일하게 data에 목록, meta에 공통 페이징 정보를 반환한다.
    // AI작성: 목록 배열과 페이징 정보를 공통 ApiResponse의 data·meta에 각각 담아 반환한다.
    @GetMapping
    public ApiResponse<List<SavingGroupSummaryResponse>> list(
            @Valid @ModelAttribute SavingGroupSearchCondition condition) {
        SavingGroupListResponse result = savingGroupService.getSavingGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    @GetMapping("/{savingGroupId}/settings")
    public ApiResponse<SavingGroupSettingsResponse> getSettings(
            @PathVariable Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        // AI작성: 설정 접근 권한은 클라이언트 입력이 아닌 JWT 필터가 검증한 회원 ID로만 판단한다.
        return ApiResponse.ok(savingGroupService.getSavingGroupSettings(savingGroupId, memberId));
    }

    @PatchMapping("/{savingGroupId}")
    public ApiResponse<Void> updateSettings(
            @PathVariable Long savingGroupId,
            @Valid @RequestBody SavingGroupSettingsPatchRequest request,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        // AI작성: 설정 수정도 조회와 같은 JWT 인증 회원 ID로 계주 권한을 다시 검증한다.
        savingGroupService.updateSavingGroupSettings(savingGroupId, memberId, request);
        return ApiResponse.ok(null);
    }
}
