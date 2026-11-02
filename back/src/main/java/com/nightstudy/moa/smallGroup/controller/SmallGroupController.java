package com.nightstudy.moa.smallGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCategoryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteParticipationRequest;
import com.nightstudy.moa.smallGroup.SmallGroupCreationService;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// AI작성: 소모임 생성·목록·상세 API를 같은 기본 경로에서 제공한다.
@RestController
@RequestMapping("/api/small-groups")
public class SmallGroupController {
    private final SmallGroupCreationService smallGroupCreationService;
    private final SmallGroupService smallGroupService;

    public SmallGroupController(
            SmallGroupCreationService smallGroupCreationService,
            SmallGroupService smallGroupService) {
        this.smallGroupCreationService = smallGroupCreationService;
        this.smallGroupService = smallGroupService;
    }

    // AI작성: 공개 조회는 인증 회원 ID를 선택적으로 전달하고 소모임 존재·MANAGER 제한·페이징은 Service에서 처리한다.
    @GetMapping("/{smallGroupId}/reviews")
    public ApiResponse<List<SmallGroupReviewListItemResponse>> getReviews(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @ModelAttribute SmallGroupReviewSearchCondition condition) {
        SmallGroupReviewListResponse result =
                smallGroupService.getSmallGroupReviews(smallGroupId, memberId, condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    // AI작성: JWT 회원 ID와 검증된 입력값만 전달하고 후기 등록 조건은 Service에서 판단한다.
    @PostMapping("/{smallGroupId}/reviews")
    public ApiResponse<Long> createReview(
            @PathVariable("smallGroupId") Long smallGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SmallGroupReviewCreateRequest request) {
        return ApiResponse.ok(smallGroupService.createSmallGroupReview(smallGroupId, memberId, request));
    }

    // AI작성: 소모임 개설 화면은 로그인한 사용자의 활동 지역만 조회한다.
    @GetMapping("/regions")
    public ApiResponse<List<SmallGroupRegionResponse>> getSmallGroupRegions() {
        return ApiResponse.ok(smallGroupCreationService.getSmallGroupRegions());
    }

    // AI작성: 공개 목록의 관심사 필터는 DB 기준 데이터에서 조회한다.
    @GetMapping("/categories")
    public ApiResponse<List<SmallGroupCategoryResponse>> getSmallGroupCategories() {
        return ApiResponse.ok(smallGroupCreationService.getSmallGroupCategories());
    }

    // AI작성: 로그인 정보나 클라이언트 전달 회원 식별자 없이 공개 목록만 반환한다.
    @GetMapping
    public ApiResponse<List<SmallGroupSummaryResponse>> list(
            @Valid @ModelAttribute SmallGroupSearchCondition condition) {
        SmallGroupListResponse result = smallGroupService.getSmallGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    // AI작성: 가입 신청 목록은 JWT 필터가 저장한 회원 ID만 Service에 전달하고 목록·메타 형식은 공통 응답을 사용한다.
    @GetMapping("/{smallGroupId}/join-requests")
    public ApiResponse<List<SmallGroupJoinRequestRow>> getJoinRequests(
            @PathVariable("smallGroupId") Long smallGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        SmallGroupJoinRequestListResponse result =
                smallGroupService.getSmallGroupJoinRequests(smallGroupId, memberId, status, page);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    // AI작성: 가입 신청자는 요청값이 아닌 JWT 필터가 검증한 회원 ID로만 식별한다.
    @PostMapping("/{smallGroupId}/join-requests")
    public ApiResponse<Void> requestJoin(
            @PathVariable("smallGroupId") Long smallGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        smallGroupService.requestSmallGroupJoin(smallGroupId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: JWT 필터가 검증한 회원 ID와 요청 본문만 Service에 전달하고 생성 권한·입력값 검증은 Service가 처리한다.
    @PostMapping("/{smallGroupId}/votes")
    public ApiResponse<SmallGroupVoteCreateResponse> createVote(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @Valid @RequestBody SmallGroupVoteCreateRequest request,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(smallGroupService.createSmallGroupVote(smallGroupId, memberId, request));
    }

    // AI작성: 목록 조회는 JWT 필터가 저장한 회원 ID만 전달하고 소모임 존재·회원 권한 판단은 Service에서 처리한다.
    @GetMapping("/{smallGroupId}/votes")
    public ApiResponse<List<SmallGroupVoteListItemResponse>> getVotes(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(smallGroupService.getSmallGroupVotes(smallGroupId, memberId));
    }

    // AI작성: JWT 회원 ID와 두 경로 식별자만 Service에 전달하고 상세 권한·소속·마감 정책은 Service가 처리한다.
    @GetMapping("/{smallGroupId}/votes/{smallGroupVoteId}")
    public ApiResponse<SmallGroupVoteDetailResponse> getVoteDetail(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @PathVariable("smallGroupVoteId") @Positive(message = "투표 ID는 1 이상이어야 합니다.") Long smallGroupVoteId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(
                smallGroupService.getSmallGroupVoteDetail(smallGroupId, smallGroupVoteId, memberId));
    }

    // AI작성: JWT 회원 ID와 선택 보기만 전달하며 투표 가능 여부·선택 변경·저장은 Service 트랜잭션에서 처리한다.
    @PostMapping("/{smallGroupId}/votes/{smallGroupVoteId}/participations")
    public ApiResponse<Void> participateVote(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @PathVariable("smallGroupVoteId") @Positive(message = "투표 ID는 1 이상이어야 합니다.") Long smallGroupVoteId,
            @Valid @RequestBody SmallGroupVoteParticipationRequest request,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        smallGroupService.participateSmallGroupVote(smallGroupId, smallGroupVoteId, memberId, request);
        return ApiResponse.ok();
    }

    // AI작성: 투표 완료 회원만 현황을 조회하도록 Service에 JWT 회원 ID와 경로 식별자를 전달한다.
    @GetMapping("/{smallGroupId}/votes/{smallGroupVoteId}/status")
    public ApiResponse<SmallGroupVoteDetailResponse> getVoteStatus(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @PathVariable("smallGroupVoteId") @Positive(message = "투표 ID는 1 이상이어야 합니다.") Long smallGroupVoteId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(
                smallGroupService.getSmallGroupVoteStatus(smallGroupId, smallGroupVoteId, memberId));
    }

    // AI작성: 삭제 요청은 JWT 회원 ID와 경로 식별자만 Service에 전달하고 소속·권한·삭제 상태 판단은 Service가 처리한다.
    @DeleteMapping("/{smallGroupId}/votes/{smallGroupVoteId}")
    public ApiResponse<Void> deleteVote(
            @PathVariable("smallGroupId") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long smallGroupId,
            @PathVariable("smallGroupVoteId") @Positive(message = "투표 ID는 1 이상이어야 합니다.") Long smallGroupVoteId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        smallGroupService.deleteSmallGroupVote(smallGroupId, smallGroupVoteId, memberId);
        return ApiResponse.ok(null);
    }

    // AI작성: 승인자는 요청 본문이 아닌 JWT 필터가 검증한 회원 ID로만 식별한다.
    @PatchMapping("/{smallGroupId}/join-requests/{crewMemberId}/approve")
    public ApiResponse<Void> approveJoinRequest(
            @PathVariable("smallGroupId") Long smallGroupId,
            @PathVariable("crewMemberId") Long crewMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        smallGroupService.approveSmallGroupJoin(smallGroupId, crewMemberId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 거절 처리자도 요청 본문이 아닌 JWT 필터가 검증한 회원 ID로만 식별한다.
    @PatchMapping("/{smallGroupId}/join-requests/{crewMemberId}/reject")
    public ApiResponse<Void> rejectJoinRequest(
            @PathVariable("smallGroupId") Long smallGroupId,
            @PathVariable("crewMemberId") Long crewMemberId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        smallGroupService.rejectSmallGroupJoin(smallGroupId, crewMemberId, memberId);
        return ApiResponse.ok();
    }

    // AI작성: 공개 상세는 비로그인 요청도 허용하므로 memberId를 선택적으로 Service에 전달한다.
    @GetMapping("/{id}")
    public ApiResponse<SmallGroupDetailResponse> detail(
            @PathVariable("id") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long id,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(smallGroupService.getSmallGroupDetail(id, memberId));
    }

    // AI작성: 설정 정보는 JWT 필터가 검증한 회원 ID를 Service에만 전달하고 권한 판단은 Service에서 처리한다.
    @GetMapping("/{id}/settings")
    public ApiResponse<SmallGroupSettingsResponse> getSettings(
            @PathVariable("id") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long id,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ApiResponse.ok(smallGroupService.getSmallGroupSettings(id, memberId));
    }

    // AI작성: 수정 권한과 정원 축소 정책은 Service가 JWT 인증 회원 ID를 기준으로 검증한다.
    @PatchMapping("/{id}")
    public ApiResponse<Void> updateSettings(
            @PathVariable("id") @Positive(message = "소모임 ID는 1 이상이어야 합니다.") Long id,
            @Valid @RequestBody SmallGroupSettingsPatchRequest request,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        smallGroupService.updateSmallGroupSettings(id, memberId, request);
        return ApiResponse.ok(null);
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
