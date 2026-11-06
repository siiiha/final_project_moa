package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeCreateRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeUpdateRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticePinRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeDetailResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailPageResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeSearchCondition;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    // AI작성: JWT 회원과 검증된 조건만 전달하며 권한·페이징 처리는 Service에 위임한다.
    @GetMapping("/{groupId}/account/transactions")
    public ApiResponse<List<com.nightstudy.moa.savingGroup.dto.SavingGroupTransactionResponse>> transactions(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @ModelAttribute com.nightstudy.moa.savingGroup.dto.SavingGroupTransactionSearchCondition condition) {
        var result = savingGroupService.getSavingGroupTransactions(groupId, memberId, condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    // AI작성: 계좌 원문 없이 회원 전용 정보를 반환하며 모임 소속 검증은 Service에서 수행한다.
    @GetMapping("/{groupId}/account")
    public ApiResponse<com.nightstudy.moa.savingGroup.dto.SavingGroupAccountResponse> account(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(savingGroupService.getSavingGroupAccount(groupId, memberId));
    }
    // AI작성: 기존 추방 경로를 연결하고 인증 사용자·소속·권한·상태 판단은 Service에 위임한다.
    @PatchMapping("/{groupId}/members/{memberId}/expel")
    public ApiResponse<Void> expelMember(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @PathVariable("memberId") @Positive(message = "회원 ID는 1 이상이어야 합니다.") Long targetMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        savingGroupService.expelApprovedSavingGroupMember(groupId, targetMemberId, memberId);
        return ApiResponse.ok();
    }
    // AI작성: 대상 회원과 역할만 전달하고 처리자·권한·모임 소속은 Service에서 검증한다.
    @PatchMapping("/{savingGroupId}/members/{memberId}/role")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @PathVariable("memberId") @Positive(message = "회원 ID는 1 이상이어야 합니다.") Long targetMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody com.nightstudy.moa.savingGroup.dto.SavingGroupMemberRoleUpdateRequest request) {
        savingGroupService.updateSavingGroupMemberRole(savingGroupId, targetMemberId, memberId, request.getRole());
        return ApiResponse.ok();
    }
    // AI작성: 검색 DTO는 기존 목록처럼 Validation 후 전달하고 인증·권한·검색 처리는 Service가 담당한다.
    @GetMapping("/{savingGroupId}/members")
    public ApiResponse<List<com.nightstudy.moa.savingGroup.dto.SavingGroupMemberResponse>> members(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @ModelAttribute com.nightstudy.moa.savingGroup.dto.SavingGroupMemberSearchCondition condition) {
        var result = savingGroupService.getSavingGroupMembers(savingGroupId, memberId, condition.getKeyword(), condition.getPage());
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }
    // AI작성: 경로는 saving_group_id이며 인증·권한·유형별 조회는 Service에 위임한다.
    @GetMapping("/{savingGroupId}/payout-requests")
    public ApiResponse<List<com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRequestResponse>> payoutRequests(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @ModelAttribute com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRequestSearchCondition condition) {
        var result = savingGroupService.getSavingGroupPayoutRequests(savingGroupId, memberId, condition.getStatus(), condition.getPage());
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }
    // AI작성: 목록 조회와 동일한 saving_group_id와 유형별 요청 ID를 전달하며 승인 본문은 받지 않는다.
    @PatchMapping("/{savingGroupId}/payout-requests/{payoutRequestId}/approve")
    public ApiResponse<Void> approvePayoutRequest(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @PathVariable("payoutRequestId") @Positive(message = "요청 ID는 1 이상이어야 합니다.") Long payoutRequestId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        savingGroupService.approveSavingGroupPayoutRequest(savingGroupId, payoutRequestId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 사유만 검증해 전달하고 권한·소속·상태 변경은 Service에 위임한다.
    @PatchMapping("/{savingGroupId}/payout-requests/{payoutRequestId}/reject")
    public ApiResponse<Void> rejectPayoutRequest(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @PathVariable("payoutRequestId") @Positive(message = "요청 ID는 1 이상이어야 합니다.") Long payoutRequestId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRejectionRequest request) {
        savingGroupService.rejectSavingGroupPayoutRequest(savingGroupId, payoutRequestId, memberId, request.getRejectReason());
        return ApiResponse.ok();
    }
    // AI작성: 요청 본문이나 작성자 입력 없이 JWT 회원과 경로 ID만 Service로 전달한다.
    @DeleteMapping("/{groupId}/notices/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @PathVariable("noticeId") @Positive(message = "공지 ID는 1 이상이어야 합니다.") Long noticeId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        savingGroupService.deleteSavingGroupNotice(groupId, noticeId, memberId);
        return ApiResponse.ok();
    }
    // AI작성: 기존 상세·고정 API와 동일하게 경로 ID는 공통 groupId이고 인증 회원은 JWT 속성만 사용한다.
    @PutMapping("/{groupId}/notices/{noticeId}")
    public ApiResponse<Long> updateNotice(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @PathVariable("noticeId") @Positive(message = "공지 ID는 1 이상이어야 합니다.") Long noticeId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SavingGroupNoticeUpdateRequest request) {
        return ApiResponse.ok(savingGroupService.updateSavingGroupNotice(groupId, noticeId, memberId,
                request.getTitle(), request.getContent()));
    }
    private final SavingGroupService savingGroupService;

    // AI작성: 목록 조회 서비스만 주입한다.
    public SavingGroupController(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    // AI작성: 명시적 고정 상태와 양수 ID만 전달하며 권한·최대 개수는 Service 트랜잭션에서 검증한다.
    @PatchMapping("/{groupId}/notices/{noticeId}/pin")
    public ApiResponse<Void> pinNotice(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @PathVariable("noticeId") @Positive(message = "공지 ID는 1 이상이어야 합니다.") Long noticeId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SavingGroupNoticePinRequest request) {
        savingGroupService.updateSavingGroupNoticePinned(groupId, noticeId, memberId, request.getPinned());
        return ApiResponse.ok();
    }

    // AI작성: 경로의 groupId는 공통 모임 ID이며 공지 소속·승인 회원 권한은 Service에서 검증한다.
    @GetMapping("/{groupId}/notices/{noticeId}")
    public ApiResponse<SavingGroupNoticeDetailResponse> noticeDetail(
            @PathVariable("groupId") @Positive(message = "모임 ID는 1 이상이어야 합니다.") Long groupId,
            @PathVariable("noticeId") @Positive(message = "공지 ID는 1 이상이어야 합니다.") Long noticeId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(savingGroupService.getSavingGroupNoticeDetail(groupId, noticeId, memberId));
    }

    // AI작성: JSON 입력을 검증하고 JWT 회원 ID만 전달하며 모임 상태·권한·저장은 Service에서 처리한다.
    @PostMapping("/{savingGroupId}/notices")
    public ApiResponse<Void> createNotice(
            @PathVariable("savingGroupId") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long savingGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SavingGroupNoticeCreateRequest request) {
        savingGroupService.createSavingGroupNotice(savingGroupId, memberId, request.getTitle(), request.getContent());
        return ApiResponse.ok();
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
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @ModelAttribute SavingGroupNoticeSearchCondition condition) {
        // AI작성: 기존 상세 데이터는 data에 유지하고 검증된 공지 조회 조건의 페이징 정보만 meta로 전달한다.
        SavingGroupDetailPageResponse result = savingGroupService.getSavingGroupDetail(id, memberId, condition);
        return ApiResponse.ok(result.getDetail(), result.getPageMeta());
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
