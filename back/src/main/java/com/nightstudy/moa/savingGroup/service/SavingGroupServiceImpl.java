package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseCategory;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassifierClient;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassificationStatus;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassificationSource;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

// AI작성: 입력 검증은 Controller에 맡기고 조회와 응답 조합만 담당한다.
@Service
public class SavingGroupServiceImpl implements SavingGroupService {
    // AI작성: 같은 읽기 스냅샷의 모임·회원·회비로 계산하며 실제 순번·회차·지급 상태는 변경하지 않는다.
    @Override
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<SavingGroupTurnOrderRecommendationResponse> getTurnOrderRecommendations(Long savingGroupId, Long memberId) {
        if (memberId == null || memberId <= 0)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (savingGroupId == null || savingGroupId <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        var group = savingGroupMapper.selectTurnOrderRecommendationContext(savingGroupId);
        if (group == null)
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        var owner = savingGroupMapper.selectSavingGroupMembership(group.getGroupId(), memberId);
        if (owner == null || !"APPROVED".equals(owner.getMembershipStatus())
                || !"OWNER".equals(owner.getMemberRole()) || !savingGroupMapper.existsMember(memberId))
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED", "승인된 계주만 순번 추천을 조회할 수 있습니다.");
        if (!"순번계".equals(group.getGroupType()))
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TYPE_INVALID", "순번계에서만 순번을 추천받을 수 있습니다.");
        if (!"01".equals(group.getStatus()) && !"02".equals(group.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED", "모집중 또는 진행중인 계모임에서만 이용할 수 있습니다.");
        // AI작성: 실제 적용 순번으로 고정 계원을 판별하되 예상액은 전체 승인 인원을 기준으로 계산한다.
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int startRound = SavingGroupTurnOrderPolicy.nextRound(group.getStatus(), group.getStartDate(), today);
        var approved = savingGroupMapper.selectTurnOrderRecommendationMembers(group.getGroupId(), savingGroupId,
                startRound > 1 ? today.withDayOfMonth(1) : null);
        var members = new java.util.ArrayList<>(approved);
        if (startRound > 1) {
            var fixed = SavingGroupTurnOrderPolicy.fixedMembers(approved, SavingGroupTurnOrderMemberRow::getMemberId,
                    SavingGroupTurnOrderMemberRow::isPaid, SavingGroupTurnOrderMemberRow::getSequenceNumber, startRound);
            members.removeIf(member -> fixed.contains(member.getMemberId()));
            if (members.size() < 2) return List.of();
        }
        if (members.isEmpty()) return List.of();
        BigDecimal dues = group.getDuesAmount();
        if (dues == null || dues.signum() <= 0 || dues.stripTrailingZeros().scale() > 0)
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DUES_INVALID", "원 단위 기본 회비를 확인해주세요.");
        // AI작성: 추천 대상에게 우선순위를 부여하지 않고 무작위로 섞는다.
        java.util.Collections.shuffle(members);
        BigDecimal extra = dues.multiply(new BigDecimal("0.01")).setScale(0, java.math.RoundingMode.DOWN);
        BigDecimal base = dues.multiply(BigDecimal.valueOf(approved.size()));
        var result = new java.util.ArrayList<SavingGroupTurnOrderRecommendationResponse>();
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            // AI작성: 인원·회비 유지 및 앞 순번 전원 지급을 가정한 예상액이며 확정 회차 금액과 무관하다.
            int round = startRound + i;
            BigDecimal expected = base.add(extra.multiply(BigDecimal.valueOf(round - 1L)));
            if (expected.compareTo(new BigDecimal("9999999999999")) > 0)
                throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DUES_INVALID", "예상 지급 금액의 허용 범위를 확인해주세요.");
            result.add(new SavingGroupTurnOrderRecommendationResponse(member.getMemberId(), member.getNickname(), round, expected));
        }
        return List.copyOf(result);
    }
    // AI작성: 모임 잠금이 같은 모임의 역할 변경을 직렬화하며 현재 회계는 잠금 읽기로 확인한다.
    @Override
    @Transactional
    public void updateSavingGroupMemberRole(Long savingGroupId, Long targetMemberId, Long memberId, String role) {
        SavingGroupJoinTargetRow target = validateSavingGroupMemberManagement(savingGroupId, memberId);
        if (targetMemberId == null || targetMemberId <= 0 || role == null || !List.of("MEMBER", "MANAGER").contains(role)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "대상 회원 번호 또는 역할이 올바르지 않습니다.");
        }
        SavingGroupMembershipRelationRow relation = savingGroupMapper
                .selectSavingGroupMembershipForUpdate(target.getGroupId(), targetMemberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || !savingGroupMapper.existsMember(targetMemberId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND",
                    "승인 상태의 계원을 찾을 수 없습니다.");
        }
        if (Objects.equals(targetMemberId, memberId) || "OWNER".equals(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_OWNER_ROLE_CHANGE_NOT_ALLOWED",
                    "계주 역할은 변경할 수 없습니다.");
        }
        if (!List.of("MEMBER", "MANAGER").contains(relation.getMemberRole() == null ? "" : relation.getMemberRole())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_MEMBER_ROLE_INVALID", "현재 계원 역할을 확인해주세요.");
        }
        // AI작성: 중복 지정은 자동 교체하지 않는다. 기존 회계를 해제한 뒤 새 회계를 지정해야 한다.
        if ("MANAGER".equals(role) && savingGroupMapper.selectSavingGroupManagerIdsForUpdate(target.getGroupId())
                .stream().anyMatch(id -> !Objects.equals(id, targetMemberId))) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_MANAGER_ALREADY_EXISTS",
                    "회계는 한 명만 지정할 수 있습니다. 기존 회계를 계원으로 변경한 뒤 지정해주세요.");
        }
        if (role.equals(relation.getMemberRole())) return;
        int affectedRows = savingGroupMapper.updateSavingGroupMemberRole(target.getGroupId(), relation.getCrewMemberId(),
                relation.getMemberRole(), role);
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_MEMBER_ROLE_UPDATE_FAILED",
                    "계원 상태가 변경되어 역할을 저장하지 못했습니다. 다시 확인해주세요.");
        }
        // AI작성: 실제 역할 변경이 저장된 경우에만 대상 회원에게 회계 지정 또는 해제 결과를 알린다.
        notificationService.send(
                targetMemberId,
                NotificationType.SAVING_GROUP_MEMBER_ROLE_CHANGED,
                "MANAGER".equals(role) ? "계모임 회계로 지정되었습니다." : "계모임 일반 계원으로 변경되었습니다.",
                "/saving-groups/" + savingGroupId
        );
    }
    // AI작성: 목록도 모임→계주 관계 잠금 순서로 권한을 확인하고 최대 두 페이지로 제한한다.
    @Override
    @Transactional
    public SavingGroupMemberListResponse getSavingGroupMembers(Long savingGroupId, Long memberId, int page) {
        // AI작성: 기존 검색 없는 호출도 같은 권한·페이지 처리로 이어간다.
        return getSavingGroupMembers(savingGroupId, memberId, null, page);
    }

    // AI작성: 정규화한 검색어를 바인딩 패턴으로 변환하고 집계·목록에 동일하게 전달한다.
    @Override
    @Transactional
    public SavingGroupMemberListResponse getSavingGroupMembers(Long savingGroupId, Long memberId, String keyword, int page) {
        if (page < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "페이지는 1 이상이어야 합니다.");
        }
        String normalizedKeyword = keyword == null || keyword.strip().isEmpty() ? null : keyword.strip();
        if (normalizedKeyword != null && normalizedKeyword.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "검색어는 20자 이내로 입력해 주세요.");
        }
        SavingGroupJoinTargetRow target = validateSavingGroupMemberManagement(savingGroupId, memberId);
        String keywordPattern = escapeKeyword(normalizedKeyword);
        int total = Math.min(20, savingGroupMapper.selectSavingGroupMemberCount(target.getGroupId(), keywordPattern));
        PageMeta meta = PageMeta.of(page, 10, total);
        List<SavingGroupMemberResponse> items = page > meta.getTotalPages() ? List.of()
                : savingGroupMapper.selectSavingGroupMembers(target.getGroupId(), keywordPattern, meta.getOffset());
        return new SavingGroupMemberListResponse(items, meta);
    }

    // AI작성: 역할 관리와 목록에 동일한 인증·모집중/진행중·승인 계주 정책을 적용한다.
    private SavingGroupJoinTargetRow validateSavingGroupMemberManagement(Long savingGroupId, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        if (!List.of("01", "02").contains(target.getStatus() == null ? "" : target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED",
                    "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        validateSavingGroupOwner(target, memberId);
        return target;
    }
    // AI작성: 기존 모임→계주 잠금 순서로 승인 관계를 확인하며 조회 이외의 금융 처리는 하지 않는다.
    @Override
    @Transactional
    public SavingGroupPayoutRequestListResponse getSavingGroupPayoutRequests(
            Long savingGroupId, Long memberId, String status, int page) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0 || page < 1
                || (status != null && !List.of("PENDING", "APPROVED", "REJECTED", "COMPLETED").contains(status))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 번호, 상태 또는 페이지가 올바르지 않습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        if (!"01".equals(target.getStatus()) && !"02".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED", "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        validateSavingGroupOwner(target, memberId);
        String groupType = savingGroupMapper.selectSavingGroupPayoutType(target.getGroupId());
        // AI작성: 미정의 모임 유형을 친목계로 추정하지 않고 차단한다.
        if (!"순번계".equals(groupType) && !"친목계".equals(groupType)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TYPE_INVALID", "계모임 종류를 확인해 주세요.");
        }
        if ("친목계".equals(groupType) && "COMPLETED".equals(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "친목계 지출 요청에는 완료 상태가 없습니다.");
        }
        int total = savingGroupMapper.selectSavingGroupPayoutRequestCount(target.getGroupId(), groupType, status);
        PageMeta meta = PageMeta.of(page, 10, total);
        // AI작성: 범위 초과는 곱셈 전에 빈 배열을 반환해 큰 페이지 값의 offset 오버플로를 방지한다.
        var items = page > meta.getTotalPages() ? List.<SavingGroupPayoutRequestResponse>of()
                : savingGroupMapper.selectSavingGroupPayoutRequests(target.getGroupId(), groupType, status, meta.getOffset());
        return new SavingGroupPayoutRequestListResponse(items, meta);
    }
    // AI작성: 모임 잠금으로 같은 모임의 요청 처리와 직렬화하며 승인으로 지급·순번·잔액은 변경하지 않는다.
    @Override
    @Transactional
    public void approveSavingGroupPayoutRequest(Long savingGroupId, Long requestId, Long memberId) {
        decideSavingGroupPayout(savingGroupId, requestId, memberId, null, true);
    }

    // AI작성: 거절에도 현재 회원 자격을 확인하되 순번·증빙 조건은 적용하지 않는다.
    @Override
    @Transactional
    public void rejectSavingGroupPayoutRequest(Long savingGroupId, Long requestId, Long memberId, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank() || rejectReason.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "거절 사유는 공백 없이 입력하고 500자 이내여야 합니다.");
        }
        decideSavingGroupPayout(savingGroupId, requestId, memberId, rejectReason.strip(), false);
    }

    private void decideSavingGroupPayout(Long savingGroupId, Long requestId, Long memberId, String reason, boolean approve) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0 || requestId == null || requestId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 또는 요청 번호가 올바르지 않습니다.");
        }
        var group = savingGroupMapper.selectPayoutDecisionGroupForUpdate(savingGroupId);
        if (group == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        if (!List.of("01", "02").contains(group.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED", "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        var owner = savingGroupMapper.selectSavingGroupMembershipForUpdate(group.getGroupId(), memberId);
        if (!Objects.equals(group.getOwnerMemberId(), memberId) || owner == null
                || !"APPROVED".equals(owner.getMembershipStatus()) || !"OWNER".equals(owner.getMemberRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED", "승인된 계주만 계금 요청을 심사할 수 있습니다.");
        }
        String type = group.getGroupType();
        if (!"순번계".equals(type) && !"친목계".equals(type)) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYOUT_GROUP_TYPE_UNSUPPORTED", "계모임 종류를 확인해 주세요.");
        }
        var request = savingGroupMapper.selectPayoutDecisionRequestForUpdate(group.getGroupId(), requestId, type);
        if (request == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PAYOUT_REQUEST_NOT_FOUND", "해당 모임의 계금 요청을 찾을 수 없습니다.");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYOUT_REQUEST_ALREADY_PROCESSED", "이미 처리된 요청입니다.");
        }
        var applicant = savingGroupMapper.selectSavingGroupMembershipForUpdate(group.getGroupId(), request.getRequesterId());
        // AI작성: 모임 탈퇴·추방 외에 회원 자체의 소프트 삭제도 기존 회원 잠금 조회로 차단한다.
        if (applicant == null || !"APPROVED".equals(applicant.getMembershipStatus())
                || !("OWNER".equals(applicant.getMemberRole()) || "MANAGER".equals(applicant.getMemberRole())
                || ("순번계".equals(type) && "MEMBER".equals(applicant.getMemberRole())))
                || savingGroupMapper.selectSavingGroupJoinMemberForUpdate(request.getRequesterId()) == null) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYOUT_REQUESTER_INELIGIBLE", "현재 요청자의 회원 자격으로 처리할 수 없습니다.");
        }
        if (approve && "순번계".equals(type)) {
            // AI작성: 심사도 요청과 같은 예약 순번 기준을 사용하고 지급 완료 회원의 재승인을 차단한다.
            Integer sequence = SavingGroupTurnOrderServiceImpl.resolveSequence(savingGroupMapper, group.getGroupId(), request.getRequesterId(),
                    savingGroupMapper.selectPayoutDecisionSequence(group.getGroupId(), request.getRequesterId()), request.getRoundNumber());
            if (sequence == null || sequence <= 0 || !sequence.equals(request.getRoundNumber())) {
                throw new ApiException(HttpStatus.CONFLICT, "PAYOUT_SEQUENCE_MISMATCH", "요청자의 지급 순번과 요청 회차가 일치하지 않습니다.");
            }
            // AI작성: 현재 회차 산정이나 지급 완료 판단을 새로 정의하지 않고 요청 회차와 순번만 검증한다.
        }
        if (approve && "친목계".equals(type) && !Boolean.TRUE.equals(request.getHasEvidence())) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYOUT_EVIDENCE_REQUIRED", "영수증이 첨부된 요청만 승인할 수 있습니다.");
        }
        if (savingGroupMapper.updatePayoutDecision(group.getGroupId(), requestId, type,
                approve ? "APPROVED" : "REJECTED", memberId, reason) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYOUT_REQUEST_ALREADY_PROCESSED", "요청 상태가 변경되었습니다. 다시 확인해 주세요.");
        }
        // AI작성: 심사 상태 변경이 성공한 뒤 요청자에게 승인 또는 반려 결과를 알린다.
        notificationService.send(
                request.getRequesterId(),
                approve ? NotificationType.SAVING_GROUP_PAYOUT_APPROVED
                        : NotificationType.SAVING_GROUP_PAYOUT_REJECTED,
                approve ? "계금·지출 요청이 승인되었습니다." : "계금·지출 요청이 반려되었습니다.",
                "/saving-groups/" + savingGroupId
        );
    }
    // AI작성: 수정·고정과 같은 모임→회원→공지 잠금 순서로 검증하고 물리 삭제 없이 삭제 시각만 저장한다.
    @Override
    @Transactional
    public void deleteSavingGroupNotice(Long savingGroupId, Long noticeId, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0 || noticeId == null || noticeId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 또는 공지 번호가 올바르지 않습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupNoticeTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        if (!"01".equals(target.getStatus()) && !"02".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED", "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        // AI작성: 잠근 계모임의 공통 ID로 회원과 공지의 소속을 확인한다.
        Long groupId = target.getGroupId();
        SavingGroupMembershipRelationRow relation = savingGroupMapper.selectSavingGroupMembershipForUpdate(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || (!"OWNER".equals(relation.getMemberRole()) && !"MANAGER".equals(relation.getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_DELETE_FORBIDDEN", "승인된 공지 작성자만 삭제할 수 있습니다.");
        }
        SavingGroupNoticeDetailResponse notice = savingGroupMapper.selectSavingGroupNoticeForUpdate(groupId, noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        }
        // AI작성: 운영진도 타인이 쓴 공지는 삭제하지 못하며 SQL에서도 인증 작성자를 다시 제한한다.
        if (!Objects.equals(notice.getWriterId(), memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_DELETE_FORBIDDEN", "공지 작성자만 삭제할 수 있습니다.");
        }
        if (savingGroupMapper.deleteSavingGroupNotice(groupId, noticeId, memberId) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SAVING_GROUP_NOTICE_DELETE_FAILED", "공지 삭제에 실패했습니다.");
        }
    }
    // AI작성: 기존 고정 변경과 같은 모임→회원 관계→공지 잠금 순서로 권한과 소속을 검증한다.
    @Override
    @Transactional
    public Long updateSavingGroupNotice(Long savingGroupId, Long noticeId, Long memberId, String title, String content) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0 || noticeId == null || noticeId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 또는 공지 번호가 올바르지 않습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupNoticeTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        if (!"01".equals(target.getStatus()) && !"02".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED", "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        // AI작성: 잠근 계모임의 공통 ID로 회원과 공지의 소속을 확인한다.
        Long groupId = target.getGroupId();
        SavingGroupMembershipRelationRow relation = savingGroupMapper.selectSavingGroupMembershipForUpdate(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || (!"OWNER".equals(relation.getMemberRole()) && !"MANAGER".equals(relation.getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_EDIT_FORBIDDEN", "승인된 공지 작성자만 공지를 수정할 수 있습니다.");
        }
        SavingGroupNoticeDetailResponse notice = savingGroupMapper.selectSavingGroupNoticeForUpdate(groupId, noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        }
        // AI작성: 사용자 확정 정책에 따라 OWNER·MANAGER도 타 작성자의 공지는 수정할 수 없다.
        if (!Objects.equals(notice.getWriterId(), memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_EDIT_FORBIDDEN", "공지 작성자만 수정할 수 있습니다.");
        }
        String nextTitle = title.trim();
        String nextContent = content.trim();
        // AI작성: 같은 내용의 재요청은 성공 처리하고 원작성자·작성일·고정 상태는 보존한다.
        if (Objects.equals(notice.getTitle(), nextTitle) && Objects.equals(notice.getContent(), nextContent)) return noticeId;
        if (savingGroupMapper.updateSavingGroupNotice(groupId, noticeId, nextTitle, nextContent) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SAVING_GROUP_NOTICE_EDIT_FAILED", "공지 수정에 실패했습니다.");
        }
        return noticeId;
    }
    private static final int PAGE_SIZE = 6;
    // AI작성: 계모임 전체 목록의 페이지 크기와 별개로 공지는 페이지당 4개를 제공한다.
    private static final int NOTICE_PAGE_SIZE = 4;
    // AI작성: 고정 영역은 일반 공지 네 개와 별도로 최대 두 개까지 허용한다.
    private static final int MAX_PINNED_NOTICES = 2;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int MAX_JOINED_SAVING_GROUPS = 5;
    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SavingGroupMapper savingGroupMapper;
    private final NotificationService notificationService;
    // AI작성: 공개 상세 응답과 회원 전용 계좌 응답의 데이터 경계를 분리한다.
    private final VirtualAccountStorageMapper accountMapper;
    private final AccountNumberCipher accountCipher;

    // AI작성: 계주 권한 확인 후 월별 회비와 완료 입금액을 읽기 전용으로 비교한다.
    @Override
    @Transactional(readOnly = true)
    public com.nightstudy.moa.savingGroup.dto.SavingGroupPaymentStatusListResponse getSavingGroupPaymentStatus(
            Long savingGroupId, Long memberId, java.time.YearMonth month, int page) {
        var access = requireSavingGroupAccountMember(savingGroupId, memberId);
        Long groupId = access.groupId();
        if (!"OWNER".equals(access.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_PAYMENT_STATUS_FORBIDDEN",
                    "계주만 회비 납부 현황을 확인할 수 있습니다.");
        }
        if (month == null || page < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "조회 월과 페이지를 확인해 주세요.");
        }
        int size = 10;
        int total = savingGroupMapper.countSavingGroupPaymentStatus(groupId);
        var meta = PageMeta.of(page, size, total);
        long offset = (page - 1L) * size;
        if (offset >= total) {
            return new com.nightstudy.moa.savingGroup.dto.SavingGroupPaymentStatusListResponse(List.of(), meta);
        }
        var start = month.atDay(1).atStartOfDay();
        var end = month.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal dues = savingGroupMapper.selectMonthlyPaymentDues(groupId, end);
        var items = savingGroupMapper.selectSavingGroupPaymentStatus(groupId, start, end, offset, size)
                .stream().map(row -> {
                    BigDecimal remaining = dues == null ? null : dues.subtract(row.depositedAmount()).max(BigDecimal.ZERO);
                    return new com.nightstudy.moa.savingGroup.dto.SavingGroupPaymentStatusResponse(
                            row.memberName(), dues, row.depositedAmount(), remaining,
                            remaining == null ? null : remaining.signum() == 0);
                }).toList();
        return new com.nightstudy.moa.savingGroup.dto.SavingGroupPaymentStatusListResponse(items, meta);
    }

    // AI작성: 계좌 조회와 거래내역 조회에 동일한 현재 모임원 권한을 적용한다.
    private AccountAccess requireSavingGroupAccountMember(Long savingGroupId, Long memberId) {
        if (memberId == null || memberId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (savingGroupId == null || savingGroupId <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 번호가 올바르지 않습니다.");
        // AI작성: 계모임 ID를 확인한 뒤 기존 계좌와 소속의 공통 모임 ID를 사용한다.
        var target = savingGroupMapper.selectSavingGroupDetailBySavingGroupId(savingGroupId);
        if (target == null)
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        Long groupId = target.getGroupId();
        var relation = savingGroupMapper.selectSavingGroupMembership(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || !List.of("OWNER", "MANAGER", "MEMBER").contains(relation.getMemberRole() == null ? "" : relation.getMemberRole()))
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_ACCOUNT_FORBIDDEN", "승인된 모임원만 계좌 정보를 확인할 수 있습니다.");
        return new AccountAccess(groupId, relation.getMemberRole());
    }

    private record AccountAccess(Long groupId, String role) {}

    @Override
    @Transactional(readOnly = true)
    public SavingGroupTransactionListResponse getSavingGroupTransactions(Long savingGroupId, Long memberId, int page) {
        Long groupId = requireSavingGroupAccountMember(savingGroupId, memberId).groupId();
        if (page < 1)
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "페이지는 1 이상이어야 합니다.");
        var account = accountMapper.selectAccount(groupId);
        if (account == null)
            throw new ApiException(HttpStatus.NOT_FOUND, "DEPOSIT_ACCOUNT_NOT_FOUND", "공용 계좌가 없습니다.");

        int size = 10;
        int total = savingGroupMapper.selectSavingGroupTransactionCount(account.getAccountId());
        var meta = PageMeta.of(page, size, total);
        long offset = (page - 1L) * size;
        if (offset >= total) return new SavingGroupTransactionListResponse(List.of(), meta);
        var items = savingGroupMapper.selectSavingGroupTransactions(account.getAccountId(), offset, size)
                .stream().map(this::toTransactionResponse).toList();
        return new SavingGroupTransactionListResponse(items, meta);
    }

    // AI작성: 최근 여섯 달의 거래를 월별로 합산하고, 미확정 확인 필요 출금은 별도 분류로 보존한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupDashboardResponse getSavingGroupDashboard(Long savingGroupId, Long memberId, YearMonth month) {
        Long groupId = requireSavingGroupAccountMember(savingGroupId, memberId).groupId();
        var account = accountMapper.selectAccount(groupId);
        if (account == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DEPOSIT_ACCOUNT_NOT_FOUND", "공용 계좌가 없습니다.");
        }
        YearMonth firstMonth = month.minusMonths(5);
        LocalDateTime start = firstMonth.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        var totals = new LinkedHashMap<YearMonth, BigDecimal[]>();
        for (int index = 0; index < 6; index += 1) totals.put(firstMonth.plusMonths(index), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        var categoryTotals = new LinkedHashMap<String, BigDecimal>();
        BigDecimal monthlyDues = BigDecimal.ZERO;
        BigDecimal monthlyExpense = BigDecimal.ZERO;
        int reviewRequiredCount = 0;

        for (var row : savingGroupMapper.selectSavingGroupDashboardTransactions(account.getAccountId(), start, end)) {
            YearMonth transactionMonth = YearMonth.from(row.transactedAt());
            BigDecimal amount = row.amount().abs();
            if ("DEPOSIT".equals(row.transactionType())) {
                totals.get(transactionMonth)[0] = totals.get(transactionMonth)[0].add(amount);
                if (transactionMonth.equals(month)) monthlyDues = monthlyDues.add(amount);
                continue;
            }
            if (!"EXPENSE".equals(row.purposeType())) continue;
            var classification = classifyDemoExpense(row);
            boolean finalCategorySaved = row.finalCategory() != null && !row.finalCategory().isBlank();
            boolean reviewRequired = !finalCategorySaved && classification != null
                    && classification.classificationStatus() == CardExpenseClassificationStatus.REVIEW_REQUIRED;
            String category = finalCategorySaved ? row.finalCategory()
                    : reviewRequired || classification == null || classification.suggestedCategory() == null
                        ? CardExpenseCategory.NEEDS_REVIEW.name() : classification.suggestedCategory().name();
            totals.get(transactionMonth)[1] = totals.get(transactionMonth)[1].add(amount);
            if (transactionMonth.equals(month)) {
                monthlyExpense = monthlyExpense.add(amount);
                categoryTotals.merge(category, amount, BigDecimal::add);
                if (reviewRequired) reviewRequiredCount += 1;
            }
        }
        var trends = new ArrayList<SavingGroupDashboardResponse.MonthlyTrend>();
        totals.forEach((trendMonth, value) -> trends.add(new SavingGroupDashboardResponse.MonthlyTrend(
                trendMonth.toString(), value[0], value[1])));
        var ratios = categoryTotals.entrySet().stream()
                .map(entry -> new SavingGroupDashboardResponse.ExpenseCategoryRatio(entry.getKey(), entry.getValue())).toList();
        return new SavingGroupDashboardResponse(savingGroupMapper.selectSavingGroupBalance(groupId), monthlyDues,
                monthlyExpense, reviewRequiredCount, trends, ratios);
    }

    @Override
    @Transactional
    public void updateSavingGroupTransactionCategories(Long savingGroupId, Long memberId,
            SavingGroupTransactionCategoryUpdateRequest request) {
        var access = requireSavingGroupAccountMember(savingGroupId, memberId);
        if (!List.of("OWNER", "MANAGER").contains(access.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_TRANSACTION_CATEGORY_FORBIDDEN",
                    "계주 또는 회계만 지출 항목을 변경할 수 있습니다.");
        }
        var account = accountMapper.selectAccount(access.groupId());
        if (account == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DEPOSIT_ACCOUNT_NOT_FOUND", "공용 계좌가 없습니다.");
        }

        for (var item : request.items()) {
            if (item.category() == CardExpenseCategory.NEEDS_REVIEW) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SAVING_GROUP_TRANSACTION_CATEGORY_INVALID",
                        "확인 필요는 최종 지출 항목으로 저장할 수 없습니다.");
            }
            var row = savingGroupMapper.selectSavingGroupTransactionForCategoryUpdate(
                    account.getAccountId(), item.transactionId());
            if (row == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_TRANSACTION_NOT_FOUND", "거래내역을 찾을 수 없습니다.");
            }
            var classification = classifyDemoExpense(row);
            if (classification == null) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CARD_EXPENSE_CLASSIFIER_UNAVAILABLE",
                        "자동 분류 서비스를 확인할 수 없습니다.");
            }
            if (classification.classificationStatus() != CardExpenseClassificationStatus.REVIEW_REQUIRED) {
                throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TRANSACTION_CATEGORY_NOT_REVIEW_REQUIRED",
                        "확인 필요한 출금만 지출 항목을 변경할 수 있습니다.");
            }
            if (savingGroupMapper.updateSavingGroupTransactionFinalCategory(
                    account.getAccountId(), item.transactionId(), item.category().name()) != 1) {
                throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TRANSACTION_CATEGORY_UPDATE_CONFLICT",
                        "거래내역이 변경되어 지출 항목을 저장하지 못했습니다.");
            }
        }
    }

    // AI작성: 잘못된 장부 행은 원본 메모나 거래 정보를 공개하지 않고 조회 오류로 처리한다.
    private SavingGroupTransactionResponse toTransactionResponse(SavingGroupTransactionRow row) {
        if (row.description() == null || row.description().isBlank() || row.balanceAfter() == null)
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SAVING_GROUP_TRANSACTION_INVALID_DATA",
                    "거래내역을 확인할 수 없습니다.");
        var classification = classifyDemoExpense(row);
        // AI작성: 사용자가 확정한 분류는 매 조회마다 계산하는 자동 분류 결과보다 우선한다.
        boolean hasFinalCategory = row.finalCategory() != null && !row.finalCategory().isBlank();
        return new SavingGroupTransactionResponse(row.transactionId(), row.transactedAt(), row.transactionType(), row.amount(),
                row.balanceAfter(), row.description(), row.purposeType(),
                hasFinalCategory ? row.finalCategory() : classification == null || classification.suggestedCategory() == null
                        ? null : classification.suggestedCategory().name(),
                hasFinalCategory ? CardExpenseClassificationStatus.CONFIRMED.name()
                        : classification == null ? null : classification.classificationStatus().name(),
                // AI작성: 자동 확정 건은 제외하고, 원래 확인이 필요했던 MOCK 출금만 재분류를 허용한다.
                classification != null && classification.classificationStatus() == CardExpenseClassificationStatus.REVIEW_REQUIRED);
    }

    // AI작성: 데모 일반 출금의 가맹점명을 분류하고 호출 실패 시에도 장부 조회를 유지한다.
    private CardExpenseClassifierClient.ClassificationResult classifyDemoExpense(SavingGroupTransactionRow row) {
        if (!"EXPENSE".equals(row.purposeType()) || !"MOCK".equals(row.sourceProvider())) {
            return null;
        }
        try {
            var saved = row.transactionId() == null ? null
                    : savingGroupMapper.selectSavingGroupTransactionClassification(row.transactionId());
            if (saved != null) return toStoredClassification(saved);
            var classification = classifierClient.classify(row.description().strip(), row.amount());
            if (row.transactionId() != null) {
                savingGroupMapper.insertSavingGroupTransactionClassification(new SavingGroupTransactionClassificationRow(
                        row.transactionId(), classification.suggestedCategory() == null ? null : classification.suggestedCategory().name(),
                        classification.classificationStatus().name(),
                        classification.confidence() == null ? null : BigDecimal.valueOf(classification.confidence()),
                        classification.classificationSource().name(), classification.reviewReason(), null));
            }
            return classification;
        } catch (ApiException exception) {
            return null;
        }
    }

    // AI작성: 저장값은 Python 응답과 같은 타입으로 복원해 기존 거래내역·대시보드 계산식을 재사용한다.
    private CardExpenseClassifierClient.ClassificationResult toStoredClassification(
            SavingGroupTransactionClassificationRow classification) {
        try {
            return new CardExpenseClassifierClient.ClassificationResult(
                    classification.suggestedCategory() == null ? null : CardExpenseCategory.valueOf(classification.suggestedCategory()),
                    classification.confidence() == null ? null : classification.confidence().doubleValue(),
                    CardExpenseClassificationStatus.valueOf(classification.classificationStatus()),
                    CardExpenseClassificationSource.valueOf(classification.predictionSource()),
                    classification.reviewReason());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "CARD_EXPENSE_CLASSIFIER_RESPONSE_INVALID",
                    "저장된 자동 분류 결과를 확인할 수 없습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGroupAccountResponse getSavingGroupAccount(Long savingGroupId, Long memberId) {
        Long groupId = requireSavingGroupAccountMember(savingGroupId, memberId).groupId();
        var account = accountMapper.selectAccount(groupId);
        if (account == null) return new SavingGroupAccountResponse(false, null, null, BigDecimal.ZERO, true);
        BigDecimal balance = savingGroupMapper.selectSavingGroupBalance(groupId);
        return new SavingGroupAccountResponse(true, account.getBankCode(), accountCipher.masked(groupId, account.getAccountNumber()),
                balance == null ? BigDecimal.ZERO : balance, true);
    }


    // AI작성: 모임 → 승인 관계 → 공지 순서로 잠그고 현재 고정 목록으로 동시 지정의 두 개 제한을 검증한다.
    @Override
    @Transactional
    public void updateSavingGroupNoticePinned(Long savingGroupId, Long noticeId, Long memberId, Boolean pinned) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0 || noticeId == null || noticeId <= 0 || pinned == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 번호, 공지 번호 또는 고정 여부가 올바르지 않습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupNoticeTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        if (!"01".equals(target.getStatus()) && !"02".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED", "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        // AI작성: 잠근 계모임의 공통 ID로 회원과 공지의 소속을 확인한다.
        Long groupId = target.getGroupId();
        SavingGroupMembershipRelationRow relation = savingGroupMapper.selectSavingGroupMembershipForUpdate(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || (!"OWNER".equals(relation.getMemberRole()) && !"MANAGER".equals(relation.getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_PIN_FORBIDDEN", "승인된 계주 또는 운영진만 공지 고정을 변경할 수 있습니다.");
        }
        SavingGroupNoticePinRow notice = savingGroupMapper.selectSavingGroupNoticePinForUpdate(groupId, noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        }
        if (notice.getPinned() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
        // AI작성: 같은 상태 재요청은 성공으로 처리하며 이미 고정된 공지를 최대 개수 때문에 거절하지 않는다.
        if (pinned.equals(notice.getPinned())) return;
        if (pinned && savingGroupMapper.selectSavingGroupPinnedNoticeIdsForUpdate(groupId).size() >= MAX_PINNED_NOTICES) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOTICE_PIN_LIMIT_EXCEEDED", "고정 공지는 최대 2개까지 지정할 수 있습니다.");
        }
        if (savingGroupMapper.updateSavingGroupNoticePinned(groupId, noticeId, pinned) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SAVING_GROUP_NOTICE_PIN_FAILED", "공지 고정 변경에 실패했습니다.");
        }
    }

    // AI작성: 공개 상세와 동일한 모임 조건을 적용한 뒤 승인 관계를 확인하고 본문을 조회한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupNoticeDetailResponse getSavingGroupNoticeDetail(Long savingGroupId, Long noticeId, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0 || noticeId == null || noticeId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 또는 공지 번호가 올바르지 않습니다.");
        }
        // AI작성: 기존 Mapper가 삭제된 모임과 상태 01·02 외의 계모임을 제외한다.
        var target = savingGroupMapper.selectSavingGroupDetailBySavingGroupId(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        Long groupId = target.getGroupId();
        SavingGroupMembershipRelationRow relation = savingGroupMapper.selectSavingGroupMembership(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_READ_FORBIDDEN",
                    "해당 계모임의 승인된 회원만 공지를 조회할 수 있습니다.");
        }
        // AI작성: 공개 상세처럼 미정의 승인 역할은 정상 권한으로 해석하지 않고 데이터 오류로 처리한다.
        if (relation.getMemberRole() == null
                || !List.of("OWNER", "MANAGER", "MEMBER").contains(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
        // AI작성: 두 ID 조건으로 조회해 없는 공지와 타 모임 공지를 동일한 404로 처리한다.
        SavingGroupNoticeDetailResponse notice = savingGroupMapper.selectSavingGroupNoticeDetail(groupId, noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        }
        return notice;
    }

    // AI작성: 모임과 회원 관계를 잠가 권한·상태 검증부터 저장까지 동일한 트랜잭션으로 처리한다.
    @Override
    @Transactional
    public void createSavingGroupNotice(Long savingGroupId, Long memberId, String title, String content) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (savingGroupId == null || savingGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        if (!"01".equals(target.getStatus()) && !"02".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED",
                    "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        // AI작성: 대상 모임의 승인 관계만 인정해 타 모임 운영진과 대기·탈퇴·추방 회원의 작성을 차단한다.
        SavingGroupMembershipRelationRow relation = savingGroupMapper.selectSavingGroupMembershipForUpdate(
                target.getGroupId(), memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || (!"OWNER".equals(relation.getMemberRole()) && !"MANAGER".equals(relation.getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_NOTICE_WRITE_FORBIDDEN",
                    "승인된 계주 또는 운영진만 공지를 작성할 수 있습니다.");
        }
        // AI작성: 요청 작성자 값 대신 인증 회원 ID를 사용하고, 게시글과 동일하게 앞뒤 공백을 제거한다.
        SavingGroupNoticeCreateRow notice = new SavingGroupNoticeCreateRow(
                target.getGroupId(), memberId, title.trim(), content.trim());
        if (savingGroupMapper.insertSavingGroupNotice(notice) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SAVING_GROUP_NOTICE_WRITE_FAILED",
                    "공지 등록에 실패했습니다.");
        }
        // AI작성: 저장 성공 후 작성자를 제외한 승인 회원에게 계모임 공지 등록 알림을 발송한다.
        savingGroupMapper.selectApprovedSavingGroupMemberIds(target.getGroupId()).stream()
                .filter(receiverId -> !Objects.equals(receiverId, memberId))
                .forEach(receiverId -> notificationService.send(
                        receiverId,
                        NotificationType.NOTICE_CREATED,
                        "계모임에 새 공지가 등록되었습니다.",
                        "/saving-groups/" + savingGroupId
                ));
    }

    // AI작성: 승인과 거절이 공유하는 잠금 검증 결과를 묶어, 처리별 추가 검증은 각각 분리한다.
    private record SavingGroupJoinDecisionTarget(
            SavingGroupJoinApprovalTargetRow request, SavingGroupJoinTargetRow target) {
    }

    // AI작성: 기존 계모임·계주 관계 잠금을 재사용해 권한 확인과 조회 사이의 소유자 변경을 막는다.
    @Override
    @Transactional
    public SavingGroupJoinRequestListResponse getSavingGroupJoinRequests(
            Long savingGroupId, Long memberId, String status, int page) {
        if (savingGroupId == null || savingGroupId <= 0 || page < 1
                || (status != null && !List.of("PENDING", "APPROVED", "REJECTED").contains(status))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호, 신청 상태 또는 페이지가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        // AI작성: 관리자 제한을 포함한 일시정지 상태에서는 가입 신청 관리 활동도 차단한다.
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED",
                    "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        validateSavingGroupOwner(target, memberId);
        int total = savingGroupMapper.selectSavingGroupJoinRequestCount(target.getGroupId(), status);
        PageMeta meta = PageMeta.of(page, 10, total);
        // AI작성: 빈 결과·범위 초과는 offset 곱셈 전에 반환하며 요청 페이지를 강제로 바꾸지 않는다.
        List<SavingGroupJoinRequestRow> items = page > meta.getTotalPages() ? List.of()
                : savingGroupMapper.selectSavingGroupJoinRequests(target.getGroupId(), status, meta.getOffset(), 10);
        return new SavingGroupJoinRequestListResponse(items, meta);
    }

    // AI작성: 목록 조회와 승인에서 동일한 계주 판별 기준을 재사용한다.
    private void validateSavingGroupOwner(SavingGroupJoinTargetRow target, Long memberId) {
        if (!Objects.equals(memberId, target.getOwnerMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED", "계주만 가입 신청을 관리할 수 있습니다.");
        }
        SavingGroupMembershipRelationRow owner = savingGroupMapper.selectSavingGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (owner == null || !"OWNER".equals(owner.getMemberRole()) || !"APPROVED".equals(owner.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED", "승인된 계주만 가입 신청을 관리할 수 있습니다.");
        }
    }

    // AI작성: 내부 검증 호출의 MANDATORY는 프록시를 거치지 않으므로 이 진입점에서 트랜잭션을 보장한다.
    @Override
    @Transactional
    public void approveSavingGroupJoin(Long savingGroupId, Long crewMemberId, Long memberId) {
        SavingGroupJoinApprovalTargetRow request = validateSavingGroupJoinApproval(savingGroupId, crewMemberId, memberId);
        int affectedRows = savingGroupMapper.approveSavingGroupJoinRequest(
                request.getCrewMemberId(), request.getGroupId(), memberId);
        // AI작성: 대기 상태인 기존 신청 한 행만 변경되어야 하며 불일치는 예외로 전체 롤백한다.
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_APPROVAL_FAILED",
                    "가입 신청 상태가 변경되어 승인할 수 없습니다. 다시 확인해주세요.");
        }
        // AI작성: 승인 상태가 저장되는 트랜잭션에서 채팅방 참여 권한을 부여한다.
        savingGroupMapper.insertSavingGroupChatMember(request.getGroupId(), request.getMemberId());
        // AI작성: 가입 승인이 완료된 후 신청 회원에게 계모임 상세 화면으로 이동할 수 있는 알림을 보낸다.
        notificationService.send(
                request.getMemberId(),
                NotificationType.SAVING_GROUP_JOIN_APPROVED,
                "계모임 가입 신청이 승인되었습니다.",
                "/saving-groups/" + savingGroupId
        );
    }

    // AI작성: 거절은 정원·가입 가능 수와 무관하므로 계주 권한·소속·대기 상태만 잠근 뒤 상태를 변경한다.
    @Override
    @Transactional
    public void rejectSavingGroupJoin(Long savingGroupId, Long crewMemberId, Long memberId) {
        SavingGroupJoinDecisionTarget decision = validatePendingSavingGroupJoinRequest(
                savingGroupId, crewMemberId, memberId, "거절");
        SavingGroupJoinApprovalTargetRow request = decision.request();
        int affectedRows = savingGroupMapper.rejectSavingGroupJoinRequest(
                request.getCrewMemberId(), request.getGroupId());
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_REJECTION_FAILED",
                    "가입 신청 상태가 변경되어 거절할 수 없습니다. 다시 확인해주세요.");
        }
        // AI작성: 가입 거절이 완료된 후 신청 회원에게 처리 결과 알림을 보낸다.
        notificationService.send(
                request.getMemberId(),
                NotificationType.SAVING_GROUP_JOIN_REJECTED,
                "계모임 가입 신청이 거절되었습니다.",
                null
        );
    }

    // AI작성: 계주 권한과 대상의 승인 관계를 같은 트랜잭션에서 잠가 확인한 뒤 추방 상태로 변경한다.
    @Override
    @Transactional
    public void expelApprovedSavingGroupMember(Long savingGroupId, Long targetMemberId, Long memberId) {
        if (savingGroupId == null || savingGroupId <= 0 || targetMemberId == null || targetMemberId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 또는 추방 대상 회원 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        // AI작성: 계모임 ID로 대상을 잠근 뒤 기존 회원 소속 검증을 수행한다.
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupExpulsionTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        // AI작성: 진행 전인 모집중 상태에서만 추방하며 진행중·종료 상태는 차단한다.
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED",
                    "모집 중인 계모임에서만 계원을 추방할 수 있습니다.");
        }
        validateSavingGroupOwner(target, memberId);

        SavingGroupMembershipRelationRow member = savingGroupMapper
                .selectSavingGroupMembershipForUpdate(target.getGroupId(), targetMemberId);
        if (member == null || !"APPROVED".equals(member.getMembershipStatus())
                || !savingGroupMapper.existsMember(targetMemberId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND",
                    "승인 상태의 계원을 찾을 수 없습니다.");
        }
        // AI작성: 본인 및 다른 OWNER 관계를 보호하고 MEMBER·MANAGER만 직접 추방한다.
        if (Objects.equals(targetMemberId, memberId) || "OWNER".equals(member.getMemberRole())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_OWNER_EXPULSION_NOT_ALLOWED",
                    "계주는 추방할 수 없습니다.");
        }

        if (!List.of("MEMBER", "MANAGER").contains(member.getMemberRole() == null ? "" : member.getMemberRole())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_MEMBER_ROLE_INVALID", "현재 계원 역할을 확인해주세요.");
        }
        // AI작성: 기존 SQL대로 역할·이력은 보존하고 승인 관계 상태만 EXPELLED로 변경한다.
        int affectedRows = savingGroupMapper.expelApprovedSavingGroupMember(member.getCrewMemberId(), target.getGroupId());
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_MEMBER_EXPULSION_FAILED",
                    "계원 상태가 변경되어 추방할 수 없습니다. 다시 확인해주세요.");
        }
        // AI작성: 계원 제외와 함께 채팅방 참여 권한을 회수한다.
        savingGroupMapper.deleteSavingGroupChatMember(target.getGroupId(), targetMemberId);
        // AI작성: 추방 사유·환불·정산 등 후속 정책은 확정 후 별도 구현한다.
        // AI작성: 추방 처리가 완료된 후 대상 회원에게 계모임 상세 화면으로 이동할 수 있는 알림을 보낸다.
        notificationService.send(
                targetMemberId,
                NotificationType.SAVING_GROUP_MEMBER_EXPELLED,
                "계모임에서 제외되었습니다.",
                null
        );
    }

    // AI작성: 계주가 승인 후 시작 전인 자신의 계모임만 삭제하도록 대상·소유자·상태를 잠근 동일 트랜잭션에서 검증한다.
    @Override
    @Transactional
    public void deleteSavingGroup(Long savingGroupId, Long memberId) {
        if (savingGroupId == null || savingGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupJoinTargetRow target = savingGroupMapper
                .selectSavingGroupDeleteTargetForUpdate(savingGroupId);
        if (target == null) {
            // AI작성: TODO: 이미 소프트 삭제된 모임 재요청의 별도 응답 정책이 확정되기 전에는 기존 비노출 조건과 같은 404로 처리한다.
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }

        validateSavingGroupDeleteOwner(target, memberId);
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETE_UNAVAILABLE",
                    "승인 후 시작 전인 계모임만 삭제할 수 있습니다.");
        }

        // AI작성: 삭제 후에는 모임 조회가 불가능하므로 상태 변경 전에 승인 회원 수신자를 확보한다.
        List<Long> notificationReceiverIds = savingGroupMapper
                .selectApprovedSavingGroupMemberIds(target.getGroupId()).stream()
                .filter(receiverId -> !Objects.equals(receiverId, memberId))
                .toList();
        int affectedRows = savingGroupMapper.softDeleteSavingGroup(target.getGroupId());
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETE_FAILED",
                    "계모임 상태가 변경되어 삭제할 수 없습니다. 다시 확인해주세요.");
        }
        // AI작성: 삭제된 상세 화면으로 이동시키지 않고 계주를 제외한 기존 승인 회원에게 결과만 알린다.
        notificationReceiverIds.forEach(receiverId -> notificationService.send(
                receiverId,
                NotificationType.SAVING_GROUP_DELETED,
                "참여 중인 계모임이 삭제되었습니다.",
                null
        ));
    }

    @Override
    @Transactional
    // AI작성: 계모임과 계주 관계를 잠가 권한·중복 신청을 확인한 뒤 삭제 심사만 등록한다.
    public void requestSavingGroupDeletion(Long savingGroupId, Long memberId, String reason) {
        if (savingGroupId == null || savingGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupDeleteTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        validateSavingGroupDeleteOwner(target, memberId);
        if ("00".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETION_APPLICATION_UNAVAILABLE",
                    "개설 심사 중인 계모임은 삭제 심사를 신청할 수 없습니다.");
        }
        if (savingGroupMapper.countPendingSavingGroupDeletionApplication(target.getGroupId()) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETION_ALREADY_PENDING",
                    "이미 처리 대기 중인 삭제 신청이 있습니다.");
        }
        if (savingGroupMapper.insertSavingGroupDeletionApplication(target.getGroupId(), reason.strip()) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETION_APPLICATION_FAILED",
                    "계모임 삭제 신청을 등록하지 못했습니다.");
        }
    }

    // AI작성: 생성 계주와 승인된 OWNER 관계가 모두 일치해야만 삭제 권한을 부여한다.
    private void validateSavingGroupDeleteOwner(SavingGroupJoinTargetRow target, Long memberId) {
        if (!Objects.equals(memberId, target.getOwnerMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED",
                    "계주만 계모임을 삭제할 수 있습니다.");
        }
        SavingGroupMembershipRelationRow owner = savingGroupMapper
                .selectSavingGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (owner == null || !"OWNER".equals(owner.getMemberRole())
                || !"APPROVED".equals(owner.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED",
                    "승인된 계주만 계모임을 삭제할 수 있습니다.");
        }
    }

    // AI작성: 계모임과 본인 관계를 잠근 뒤 모집중인 승인 계원만 LEFT 상태로 바꾼다.
    @Override
    @Transactional
    public void leaveSavingGroup(Long savingGroupId, Long memberId) {
        if (savingGroupId == null || savingGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_LEAVE_UNAVAILABLE",
                    "모집중인 계모임에서만 탈퇴할 수 있습니다.");
        }

        SavingGroupMembershipRelationRow member = savingGroupMapper
                .selectSavingGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (member == null || !"APPROVED".equals(member.getMembershipStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND",
                    "승인 상태의 계원을 찾을 수 없습니다.");
        }

        // AI작성: 계주 탈퇴를 막고 승인된 일반 계원과 회계만 탈퇴를 허용한다.
        if (!"MEMBER".equals(member.getMemberRole()) && !"MANAGER".equals(member.getMemberRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_LEAVE_FORBIDDEN",
                    "일반 계원과 회계만 계모임에서 탈퇴할 수 있습니다.");
        }

        int affectedRows = savingGroupMapper.leaveApprovedSavingGroupMember(
                member.getCrewMemberId(), target.getGroupId());
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_MEMBER_LEAVE_FAILED",
                    "계원 상태가 변경되어 탈퇴할 수 없습니다. 다시 확인해주세요.");
        }
        // AI작성: 계모임 탈퇴와 함께 채팅방 참여 권한을 회수한다.
        savingGroupMapper.deleteSavingGroupChatMember(target.getGroupId(), memberId);
    }

    // AI작성: 후속 승인 처리의 트랜잭션에 반드시 참여하며 이 단계에서는 저장하지 않는다.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SavingGroupJoinApprovalTargetRow validateSavingGroupJoinApproval(
            Long savingGroupId, Long crewMemberId, Long memberId) {
        SavingGroupJoinDecisionTarget decision = validatePendingSavingGroupJoinRequest(
                savingGroupId, crewMemberId, memberId, "승인");
        SavingGroupJoinApprovalTargetRow request = decision.request();
        SavingGroupJoinTargetRow target = decision.target();
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_UNAVAILABLE", "현재 가입 승인할 수 없는 계모임입니다.");
        }
        if (target.getMaxMembers() == null || target.getMaxMembers() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_NOT_CONFIGURED", "계모임 정원이 설정되지 않았습니다.");
        }
        if (savingGroupMapper.selectApprovedMemberCountForUpdate(target.getGroupId()) >= target.getMaxMembers()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_FULL", "계모임 정원이 모두 찼습니다.");
        }
        if (savingGroupMapper.selectApprovedSavingGroupCount(request.getMemberId()) >= MAX_JOINED_SAVING_GROUPS) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_LIMIT_EXCEEDED", "가입 가능한 계모임 수는 최대 5개입니다.");
        }
        return request;
    }

    // AI작성: 신청 회원 → 계모임 → 신청 행 순서로 잠가 승인·거절의 공통 권한·소속·대기 상태를 재검증한다.
    private SavingGroupJoinDecisionTarget validatePendingSavingGroupJoinRequest(
            Long savingGroupId, Long crewMemberId, Long memberId, String actionName) {
        if (savingGroupId == null || savingGroupId <= 0 || crewMemberId == null || crewMemberId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 또는 가입 신청 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupJoinApprovalTargetRow identified = savingGroupMapper.selectSavingGroupJoinApprovalTarget(crewMemberId);
        if (identified == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "가입 신청을 찾을 수 없습니다.");
        }
        SavingGroupJoinMemberRow applicant = savingGroupMapper.selectSavingGroupJoinMemberForUpdate(identified.getMemberId());
        if (applicant == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_MEMBER_NOT_FOUND", "신청 회원을 찾을 수 없습니다.");
        }
        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        SavingGroupJoinApprovalTargetRow request = savingGroupMapper.selectSavingGroupJoinApprovalTargetForUpdate(crewMemberId);
        if (request == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "가입 신청을 찾을 수 없습니다.");
        }
        if (!Objects.equals(request.getMemberId(), identified.getMemberId())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_CHANGED", "가입 신청이 변경되었습니다. 다시 확인해주세요.");
        }
        if (!Objects.equals(request.getGroupId(), target.getGroupId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "해당 계모임의 가입 신청을 찾을 수 없습니다.");
        }
        validateSavingGroupOwner(target, memberId);
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED",
                    "현재 상태에서는 계모임 활동을 이용할 수 없습니다.");
        }
        if (!"PENDING".equals(request.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_NOT_PENDING",
                    "대기 중인 가입 신청만 " + actionName + "할 수 있습니다.");
        }
        return new SavingGroupJoinDecisionTarget(request, target);
    }

    // AI작성: 기존 프로젝트의 생성자 주입 방식을 사용한다.
    private final CardExpenseClassifierClient classifierClient;

    public SavingGroupServiceImpl(
            SavingGroupMapper savingGroupMapper,
            VirtualAccountStorageMapper accountMapper,
            AccountNumberCipher accountCipher,
            NotificationService notificationService,
            CardExpenseClassifierClient classifierClient
    ) {
        this.savingGroupMapper = savingGroupMapper;
        this.accountMapper = accountMapper;
        this.accountCipher = accountCipher;
        this.notificationService = notificationService;
        this.classifierClient = classifierClient;
    }

    // AI작성: 대상 조건을 먼저 확인하고 승인 계원·공지·역할을 같은 읽기 트랜잭션에서 조합한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupDetailResponse getSavingGroupDetail(Long savingGroupId, Long memberId) {
        // AI작성: 기존 Controller와 호출자는 최신순 1페이지로 동작하도록 기존 메서드를 유지한다.
        return getSavingGroupDetail(savingGroupId, memberId, new SavingGroupNoticeSearchCondition()).getDetail();
    }

    // AI작성: 개수와 목록을 같은 읽기 트랜잭션에서 조회하고 기존 상세의 역할·가입 제한 처리를 유지한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupDetailPageResponse getSavingGroupDetail(
            Long savingGroupId, Long memberId, SavingGroupNoticeSearchCondition condition) {
        // AI작성: Controller를 거치지 않는 호출에서도 잘못된 조건과 경로 ID를 차단한다.
        if (savingGroupId == null || savingGroupId <= 0 || condition == null
                || condition.getNoticePage() == null || condition.getNoticePage() < 1
                || condition.getNoticeSort() == null
                || !List.of("LATEST", "OLDEST").contains(condition.getNoticeSort())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 번호, 공지 페이지 또는 정렬이 올바르지 않습니다.");
        }
        SavingGroupRow row = savingGroupMapper.selectSavingGroupDetailBySavingGroupId(savingGroupId);
        if (row == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        // AI작성: 계모임 ID로 찾은 공통 모임 ID를 소속과 공지 조회에 사용한다.
        Long groupId = row.getGroupId();
        long count = savingGroupMapper.selectDetailMemberCount(groupId);
        int noticeTotal = savingGroupMapper.selectSavingGroupNoticeCount(groupId);
        PageMeta pageMeta = PageMeta.of(condition.getNoticePage(), NOTICE_PAGE_SIZE, noticeTotal);
        // AI작성: 빈 목록과 범위 초과는 요청 페이지를 유지하고 offset 계산을 생략해 정수 곱셈 오버플로를 방지한다.
        List<SavingGroupNoticeSummaryResponse> notices = pageMeta.getPage() > pageMeta.getTotalPages()
                ? List.of() : savingGroupMapper.selectSavingGroupNotices(
                        groupId, condition.getNoticeSort(), pageMeta.getOffset());
        String role = null;
        if (memberId != null) {
            List<String> roles = savingGroupMapper.selectApprovedMemberRoles(groupId, memberId);
            // AI작성: 충돌하는 승인 역할이나 미정의 코드를 임의로 선택해 권한을 확대하지 않는다.
            if (roles.size() > 1 || (!roles.isEmpty()
                    && !List.of("OWNER", "MANAGER", "MEMBER").contains(roles.get(0)))) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
            }
            if (!roles.isEmpty()) role = roles.get(0);
        }
        // AI작성: 공개 조회에서도 본인 관계만 조회해 신청 제한을 안내한다.
        SavingGroupMembershipRelationRow relation = memberId == null ? null
                : savingGroupMapper.selectSavingGroupMembership(groupId, memberId);
        String membershipStatus = relation == null ? null : relation.getMembershipStatus();
        JoinAvailability availability = evaluateSavingGroupJoin(row, memberId, relation, count);
        SavingGroupDetailResponse detail = new SavingGroupDetailResponse(row.getSavingGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getMaxMembers(), count, row.getDuesAmount(), row.getDuesDay(),
                row.getStatus(), notices, role, membershipStatus, availability.canApply(),
                availability.code(), availability.message());
        // AI작성: 고정 공지는 페이지·일반 정렬과 관계없이 별도로 반환하고 PageMeta 집계에는 포함하지 않는다.
        detail.setPinnedNotices(savingGroupMapper.selectSavingGroupPinnedNotices(groupId));
        // AI작성: 실제 상세 응답에도 유형을 포함해 순번계 전용 추천 버튼을 노출한다.
        detail.setGroupType(row.getGroupType());
        return new SavingGroupDetailPageResponse(detail, pageMeta);
    }

    // AI작성: 모임장을 포함한 승인 회원을 일괄 집계하고 빈 결과는 정상 반환한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition request) {
        String regionName = request.getRegionCode() == null ? null
                : REGIONS.get(Integer.parseInt(request.getRegionCode()) - 1);
        SavingGroupListCondition condition = new SavingGroupListCondition(
                // AI작성: 기존 코드 검색을 보존하고 새 지역 조건은 선택한 단계까지만 전달한다.
                escapeKeyword(request.getKeyword()), regionName,
                request.getProvinceName(), request.getDistrictName(), request.getTownName(),
                0, PAGE_SIZE);
        int total = savingGroupMapper.selectSavingGroupCount(condition);
        PageMeta pageMeta = PageMeta.of(request.getPage(), PAGE_SIZE, total);
        // AI작성: 범위 초과는 offset 계산 전에 반환하여 큰 페이지 번호의 int 곱셈을 피한다.
        if (pageMeta.getPage() > pageMeta.getTotalPages()) {
            return new SavingGroupListResponse(List.of(), pageMeta);
        }
        condition.setOffset(pageMeta.getOffset());
        List<SavingGroupRow> rows = savingGroupMapper.selectSavingGroupList(condition);
        Map<Long, Long> counts = rows.isEmpty() ? Map.of()
                : savingGroupMapper.selectApprovedMemberCounts(
                                rows.stream().map(SavingGroupRow::getGroupId).distinct().toList())
                        .stream().collect(Collectors.toMap(
                                SavingGroupMemberCount::getGroupId, SavingGroupMemberCount::getMemberCount));
        List<SavingGroupSummaryResponse> items = rows.stream().map(row ->
                new SavingGroupSummaryResponse(row.getSavingGroupId(), row.getGroupId(), row.getGroupName(),
                        row.getDescription(), row.getRegionName(), row.getMaxMembers(),
                        counts.getOrDefault(row.getGroupId(), 0L), row.getDuesAmount(),
                        row.getDuesDay(), row.getStatus())).toList();
        return new SavingGroupListResponse(items, pageMeta);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGroupSettingsResponse getSavingGroupSettings(Long savingGroupId, Long loginMemberId) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupSettingsAuthorityRow authority =
                savingGroupMapper.selectSavingGroupSettingsAuthority(savingGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        boolean isOwner = Objects.equals(authority.getGroupLeaderMemberId(), loginMemberId)
                && Objects.equals(authority.getOwnerMemberId(), loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_SETTINGS_FORBIDDEN",
                    "계주만 설정을 조회할 수 있습니다.");
        }

        if ("03".equals(authority.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_ACTIVITY_RESTRICTED",
                    "이용 제한된 계모임의 설정은 조회할 수 없습니다.");
        }

        if ("04".equals(authority.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "종료된 계모임의 설정은 조회할 수 없습니다.");
        }

        List<String> editableFields = getEditableFields(authority.getStatus());
        if (editableFields == null) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "현재 상태에서는 계모임 설정을 조회할 수 없습니다.");
        }

        SavingGroupCurrentSettingsResponse current =
                savingGroupMapper.selectSavingGroupCurrentSettings(savingGroupId);
        if (current == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        // TODO: 예정 설정 테이블 추가 후 적용 예정 설정을 별도 Mapper 함수로 조회한다.

        return new SavingGroupSettingsResponse(
                authority.getSavingGroupId(),
                authority.getStatus(),
                true,
                !editableFields.isEmpty(),
                current,
                null,
                editableFields
        );
    }

    @Override
    @Transactional
    public void updateSavingGroupSettings(
            Long savingGroupId, Long loginMemberId, SavingGroupSettingsPatchRequest request) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupSettingsAuthorityRow authority =
                savingGroupMapper.selectSavingGroupSettingsAuthorityForUpdate(savingGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        Long ownerMemberId = savingGroupMapper.selectSavingGroupOwnerMemberIdForUpdate(authority.getGroupId());
        boolean isOwner = Objects.equals(authority.getGroupLeaderMemberId(), loginMemberId)
                && Objects.equals(ownerMemberId, loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_SETTINGS_FORBIDDEN",
                    "계주만 설정을 변경할 수 있습니다.");
        }

        validateEditableFields(authority.getStatus(), request);
        normalizeBasicSettings(request);

        if (hasBasicSettingsUpdate(request)) {
            SavingGroupSettingsBasicUpdate basicUpdate =
                    createBasicSettingsUpdate(authority.getGroupId(), request);

            if (basicUpdate.getMaxMembers() != null) {
                long approvedMemberCount =
                        savingGroupMapper.selectApprovedMemberCountForUpdate(authority.getGroupId());
                if (basicUpdate.getMaxMembers() < approvedMemberCount) {
                    throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_BELOW_MEMBERS",
                            "승인된 계원 수보다 작게 설정할 수 없습니다.");
                }
            }

            savingGroupMapper.updateSavingGroupBasicSettings(basicUpdate);
        }

        updateDuesSettings(authority, request);
    }

    @Override
    @Transactional
    public void applyDueSettingChanges() {
        LocalDateTime appliedAt = LocalDateTime.now(KOREA_ZONE_ID);
        List<SavingGroupSettingChangeRow> candidates =
                savingGroupMapper.selectDueSettingChanges(appliedAt);

        for (SavingGroupSettingChangeRow candidate : candidates) {
            String savingGroupStatus =
                    savingGroupMapper.selectSavingGroupStatusForUpdate(candidate.getGroupId());
            if (!"02".equals(savingGroupStatus)) {
                continue;
            }

            SavingGroupSettingChangeRow change = savingGroupMapper.selectSavingGroupSettingChangeForApply(
                    candidate.getSettingChangeId(), appliedAt);
            if (change == null) {
                continue;
            }

            savingGroupMapper.applySavingGroupSettingChange(change);
            savingGroupMapper.markSavingGroupSettingChangeApplied(
                    change.getSettingChangeId(), appliedAt);
        }
    }

    @Override
    @Transactional
    public void changeSavingGroupStatus(Long groupId, String nextStatus) {
        if (nextStatus == null || !List.of("00", "01", "02", "03", "04").contains(nextStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAVING_GROUP_STATUS_INVALID",
                    "계모임 상태를 확인해주세요.");
        }

        String currentStatus = savingGroupMapper.selectSavingGroupStatusForUpdate(groupId);
        if (currentStatus == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        if (Objects.equals(currentStatus, nextStatus)) {
            return;
        }

        int updatedCount = savingGroupMapper.updateSavingGroupStatus(groupId, nextStatus);
        if (updatedCount != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        LocalDateTime changedAt = LocalDateTime.now(KOREA_ZONE_ID);
        if ("02".equals(currentStatus) && "03".equals(nextStatus)) {
            savingGroupMapper.holdPendingSavingGroupSettingChanges(groupId);
            return;
        }

        if ("04".equals(nextStatus)) {
            savingGroupMapper.cancelUnappliedSavingGroupSettingChanges(groupId, changedAt);
            return;
        }

        if ("03".equals(currentStatus) && "02".equals(nextStatus)) {
            savingGroupMapper.resumeHeldSavingGroupSettingChanges(
                    groupId, changedAt, getNextMonthEffectiveAt(changedAt));
        }
    }

    private void validateEditableFields(String status, SavingGroupSettingsPatchRequest request) {
        if ("03".equals(status) || "04".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "현재 상태에서는 계모임 정보를 수정할 수 없습니다.");
        }

        if (!"00".equals(status) && !"01".equals(status) && !"02".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "현재 상태에서는 계모임 정보를 수정할 수 없습니다.");
        }

        if ("02".equals(status) && request.isMaxMembersProvided()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_FIELD_NOT_EDITABLE",
                    "진행 중인 계모임에서는 정원을 수정할 수 없습니다.");
        }

    }

    private void normalizeBasicSettings(SavingGroupSettingsPatchRequest request) {
        if (request.isGroupNameProvided()) {
            request.setGroupName(request.getGroupName().strip());
        }
        if (request.isDescriptionProvided()) {
            request.setDescription(request.getDescription().strip());
        }
    }

    private SavingGroupSettingsBasicUpdate createBasicSettingsUpdate(
            Long groupId, SavingGroupSettingsPatchRequest request) {
        String groupName = request.isGroupNameProvided() ? request.getGroupName() : null;
        String description = request.isDescriptionProvided() ? request.getDescription() : null;
        Integer maxMembers = request.isMaxMembersProvided() ? request.getMaxMembers() : null;

        return new SavingGroupSettingsBasicUpdate(
                groupId, groupName, description, maxMembers);
    }

    private boolean hasBasicSettingsUpdate(SavingGroupSettingsPatchRequest request) {
        return request.isGroupNameProvided()
                || request.isDescriptionProvided()
                || request.isMaxMembersProvided();
    }

    private void updateDuesSettings(
            SavingGroupSettingsAuthorityRow authority, SavingGroupSettingsPatchRequest request) {
        if (!request.isDuesAmountProvided() && !request.isDuesDayProvided()) {
            return;
        }

        BigDecimal duesAmount = request.isDuesAmountProvided()
                ? BigDecimal.valueOf(request.getDuesAmount()) : null;
        Integer duesDay = request.isDuesDayProvided() ? request.getDuesDay() : null;

        if ("02".equals(authority.getStatus())) {
            SavingGroupSettingChangeRow change = new SavingGroupSettingChangeRow();
            change.setGroupId(authority.getGroupId());
            change.setDuesAmount(duesAmount);
            change.setDuesDay(duesDay);
            change.setEffectiveAt(getNextMonthEffectiveAt());
            savingGroupMapper.upsertSavingGroupSettingChange(change);
            return;
        }

        savingGroupMapper.updateSavingGroupCurrentDues(
                new SavingGroupCurrentDuesUpdate(authority.getSavingGroupId(), duesAmount, duesDay));
    }

    private LocalDateTime getNextMonthEffectiveAt() {
        return getNextMonthEffectiveAt(LocalDateTime.now(KOREA_ZONE_ID));
    }

    private LocalDateTime getNextMonthEffectiveAt(LocalDateTime referenceAt) {
        return YearMonth.from(referenceAt).plusMonths(1).atDay(1).atStartOfDay();
    }

    private List<String> getEditableFields(String status) {
        return switch (status) {
            case "00", "01" -> List.of("groupName", "description", "maxMembers", "duesAmount", "duesDay");
            case "02" -> List.of("groupName", "description", "duesAmount", "duesDay");
            case "03" -> List.of();
            default -> null;
        };
    }

    // AI작성: 회원과 계모임을 같은 트랜잭션에서 잠가 중복 신청·정원·가입 수 검증 뒤에만 대기 상태를 저장한다.
    @Override
    @Transactional
    public void requestSavingGroupJoin(Long savingGroupId, Long memberId) {
        if (savingGroupId == null || savingGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        // AI작성: 회원 잠금을 먼저 획득해 같은 회원의 여러 모임 동시 신청이 5개 제한을 우회하지 못하게 한다.
        SavingGroupJoinMemberRow member = savingGroupMapper.selectSavingGroupJoinMemberForUpdate(memberId);
        validateSavingGroupJoinMember(member);

        SavingGroupJoinTargetRow target = savingGroupMapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        }
        if (!"01".equals(target.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_UNAVAILABLE",
                    "현재 가입 신청할 수 없는 계모임입니다.");
        }
        if (target.getMaxMembers() == null || target.getMaxMembers() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_NOT_CONFIGURED",
                    "계모임 정원이 설정되지 않았습니다.");
        }

        SavingGroupMembershipRelationRow relation = savingGroupMapper
                .selectSavingGroupMembershipForUpdate(target.getGroupId(), memberId);
        boolean isReapplication = isLeftMembership(relation);
        validateExistingMembership(relation);

        validateSavingGroupJoinCapacity(target.getMaxMembers(),
                savingGroupMapper.selectApprovedMemberCountForUpdate(target.getGroupId()));

        long joinedSavingGroupCount = savingGroupMapper.selectApprovedSavingGroupCount(memberId);
        if (joinedSavingGroupCount >= MAX_JOINED_SAVING_GROUPS) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_LIMIT_EXCEEDED",
                    "가입 가능한 계모임 수는 최대 5개입니다.");
        }

        try {
            int affectedRows = isReapplication
                    ? savingGroupMapper.reactivateLeftSavingGroupMembership(target.getGroupId(), memberId)
                    : savingGroupMapper.insertSavingGroupJoinRequest(target.getGroupId(), memberId);
            if (affectedRows != 1) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "JOIN_REQUEST_CREATE_FAILED",
                        "가입 신청 처리에 실패했습니다.");
            }
        } catch (DuplicateKeyException e) {
            // AI작성: 유니크 제약 충돌은 서버 오류가 아닌 같은 모임의 중복 신청으로 응답한다.
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_DUPLICATED",
                    "이미 가입 신청한 계모임입니다.");
        }

        // AI작성: 가입 신청 저장이 완료된 후 계주에게 새로운 신청이 접수되었음을 알린다.
        notificationService.send(
                target.getOwnerMemberId(),
                NotificationType.SAVING_GROUP_JOIN_REQUEST,
                "새로운 계모임 가입 신청이 접수되었습니다.",
                "/saving-groups/" + savingGroupId + "?menu=members"
        );
    }

    // AI작성: 상세 조회와 실제 신청에서 계정·본인 인증 조건을 동일하게 검사한다.
    private void validateSavingGroupJoinMember(SavingGroupJoinMemberRow member) {
        if (member == null || !ACTIVE_ACCOUNT_STATUS.equals(member.getAccountStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_ACCOUNT_UNAVAILABLE",
                    "현재 계정 상태에서는 가입 신청할 수 없습니다.");
        }
        if (member.getIdentityVerifiedAt() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "IDENTITY_VERIFICATION_REQUIRED",
                    "본인 인증 완료 후 가입 신청할 수 있습니다.");
        }
    }

    // AI작성: 정원은 승인 인원으로만 판단하고 상세·신청에서 같은 조건을 사용한다.
    private void validateSavingGroupJoinCapacity(Integer maxMembers, long memberCount) {
        if (maxMembers == null || maxMembers <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_NOT_CONFIGURED", "계모임 정원이 설정되지 않았습니다.");
        }
        if (memberCount >= maxMembers) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_FULL", "계모임 정원이 모두 찼습니다.");
        }
    }

    // AI작성: 제한은 정상 상세 응답에 담고 데이터 오류는 숨기지 않고 기존 예외로 전달한다.
    private record JoinAvailability(boolean canApply, String code, String message) {}

    private JoinAvailability evaluateSavingGroupJoin(SavingGroupRow row, Long memberId, SavingGroupMembershipRelationRow relation, long memberCount) {
        try {
            if (memberId == null || memberId <= 0) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
            }
            validateExistingMembership(relation);
            validateSavingGroupJoinMember(savingGroupMapper.selectSavingGroupJoinMember(memberId));
            if (!"01".equals(row.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_UNAVAILABLE", "현재 가입 신청할 수 없는 계모임입니다.");
            }
            validateSavingGroupJoinCapacity(row.getMaxMembers(), memberCount);
            if (savingGroupMapper.selectApprovedSavingGroupCountReadOnly(memberId) >= MAX_JOINED_SAVING_GROUPS) {
                throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_LIMIT_EXCEEDED", "가입 가능한 계모임 수는 최대 5개입니다.");
            }
            return new JoinAvailability(true, null, null);
        } catch (ApiException error) {
            if (error.getStatus().is5xxServerError()) throw error;
            return new JoinAvailability(false, error.getCode(), error.getMessage());
        }
    }

    private boolean isLeftMembership(SavingGroupMembershipRelationRow relation) {
        return relation != null && "LEFT".equals(relation.getMembershipStatus());
    }

    private void validateExistingMembership(SavingGroupMembershipRelationRow relation) {
        if (relation == null || "LEFT".equals(relation.getMembershipStatus())) {
            return;
        }
        if ("APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_JOINED_SAVING_GROUP", "이미 가입된 계모임입니다.");
        }
        if ("PENDING".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_ALREADY_PENDING",
                    "이미 가입 신청한 계모임입니다.");
        }
        // AI작성: 정책은 같아도 거절과 추방의 안내 문구는 구분한다.
        if ("REJECTED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REAPPLICATION_FORBIDDEN",
                    "가입 신청이 거절되어 이 모임에 다시 신청할 수 없습니다.");
        }
        if ("EXPELLED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REAPPLICATION_FORBIDDEN",
                    "추방된 모임에는 다시 가입 신청할 수 없습니다.");
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBERSHIP_STATUS_INVALID",
                "가입 상태 정보를 처리할 수 없습니다.");
    }

    // AI작성: 바인딩과 별개로 LIKE 특수문자를 일반 문자로 검색하도록 처리한다.
    private String escapeKeyword(String keyword) {
        return keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
