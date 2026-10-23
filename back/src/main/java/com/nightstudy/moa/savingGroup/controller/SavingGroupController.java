package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSummaryResponse;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// AI작성: /api 경로는 기존 프론트 클라이언트와 개발 프록시를 위한 동일 API 별칭이다.
@RestController
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class SavingGroupController {
    private final SavingGroupService savingGroupService;

    // AI작성: 목록 조회와 가입 신청 승인에 기존 Service를 사용한다.
    public SavingGroupController(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    // AI작성: JWT 연결 전에는 공개 조회만 제공하며 클라이언트의 사용자 ID·역할은 받지 않는다.
    @GetMapping("/{id}")
    public ApiResponse<SavingGroupDetailResponse> detail(
            @PathVariable("id") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long id) {
        return ApiResponse.ok(savingGroupService.getSavingGroupDetail(id, null));
    }

    // AI작성: TODO: JWT 병합 후 두 목록 경로에도 인증 인터셉터를 적용한다. 인증 속성이 없으면 조회하지 않는다.
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

    // AI작성: 승인자는 서버 인증 정보로만 받아 기존 트랜잭션 Service에 전달한다. 요청 본문은 사용하지 않는다.
    @PatchMapping("/{savingGroupId}/join-requests/{crewMemberId}/approve")
    public ApiResponse<Void> approveJoinRequest(
            @PathVariable("savingGroupId") Long savingGroupId,
            @PathVariable("crewMemberId") Long crewMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        // AI작성: TODO: JWT 브랜치 병합 후 LoginInterceptor가 두 승인 경로를 검증하고 memberId를 설정하도록 WebConfig에 등록한다.
        // AI작성: 인증 연결 전에는 401로 차단한다. 헤더·쿼리·본문의 회원 ID나 임시 고정 ID를 인증 정보로 사용하지 않는다.
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.approveSavingGroupJoin(savingGroupId, crewMemberId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 추방 대상은 URL의 groupId·memberId로 받고, 실행 계주는 서버 인증 속성으로만 식별한다.
    @PatchMapping("/{groupId}/members/{memberId}/expel")
    public ApiResponse<Void> expelApprovedMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("memberId") Long targetMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.expelApprovedSavingGroupMember(groupId, targetMemberId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 탈퇴 대상은 서버 인증 회원으로 고정하고 URL·본문에서 다른 회원 ID를 받지 않는다.
    @DeleteMapping("/{savingGroupId}/members/me")
    public ApiResponse<Void> leaveSavingGroup(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        // AI작성: JWT 병합 후 LoginInterceptor가 memberId를 설정하도록 이 경로도 인증 대상에 등록해야 한다.
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.leaveSavingGroup(savingGroupId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 삭제 대상은 URL ID로 받고, 실행 계주는 서버 인증 계층이 설정한 요청 속성으로만 식별한다.
    @DeleteMapping("/{savingGroupId}")
    public ApiResponse<Void> deleteSavingGroup(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        // AI작성: TODO: JWT 브랜치 병합 후 LoginInterceptor가 memberId를 설정하도록 이 경로를 인증 대상에 등록한다.
        // AI작성: 인증 연동 전에는 헤더·쿼리·본문의 회원 ID나 임시 고정 ID를 사용하지 않고 401로 차단한다.
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        savingGroupService.deleteSavingGroup(savingGroupId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 게시글과 동일하게 data에 목록, meta에 공통 페이징 정보를 반환한다.
    @GetMapping
    public ApiResponse<List<SavingGroupSummaryResponse>> list(
            @Valid @ModelAttribute SavingGroupSearchCondition condition) {
        SavingGroupListResponse result = savingGroupService.getSavingGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    @GetMapping("/{savingGroupId}/settings")
    public ApiResponse<SavingGroupSettingsResponse> getSettings(
            @PathVariable Long savingGroupId) {
        // TODO: JWT 인증 완료 후 SecurityContext에서 로그인 회원 ID를 추출한다.
        Long loginMemberId = 67L;
        return ApiResponse.ok(savingGroupService.getSavingGroupSettings(savingGroupId, loginMemberId));
    }

    @PatchMapping("/{savingGroupId}")
    public ApiResponse<Void> updateSettings(
            @PathVariable Long savingGroupId,
            @Valid @RequestBody SavingGroupSettingsPatchRequest request) {
        // TODO: JWT 인증 완료 후 SecurityContext에서 로그인 회원 ID를 추출한다.
        Long loginMemberId = 67L;
        savingGroupService.updateSavingGroupSettings(savingGroupId, loginMemberId, request);
        return ApiResponse.ok(null);
    }

    // AI작성: 인증 연동 전 기존 설정 API와 동일한 임시 로그인 회원으로 가입 신청 서비스만 호출한다.
    @PostMapping("/{savingGroupId}/join-requests")
    public ApiResponse<Void> requestJoin(
            @PathVariable @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId) {
        // TODO: JWT 인증 완료 후 SecurityContext에서 로그인 회원 ID를 추출한다.
        Long loginMemberId = 67L;
        savingGroupService.requestSavingGroupJoin(savingGroupId, loginMemberId);
        return ApiResponse.ok();
    }
}
