package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.Objects;

// AI작성: 입력 검증은 Controller에 맡기고 조회와 응답 조합만 담당한다.
@Service
public class SavingGroupServiceImpl implements SavingGroupService {
    private static final int PAGE_SIZE = 6;
    private static final int MAX_JOINED_SAVING_GROUPS = 5;
    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SavingGroupMapper savingGroupMapper;

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
    }

    // AI작성: 후속 승인 처리의 트랜잭션에 반드시 참여하며 이 단계에서는 저장하지 않는다.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SavingGroupJoinApprovalTargetRow validateSavingGroupJoinApproval(
            Long savingGroupId, Long crewMemberId, Long memberId) {
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
        // AI작성: 초기 조회는 식별용이다. 신청 회원 → 계모임 순서로 잠근 뒤 신청 행을 재검증한다.
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
        if (!"PENDING".equals(request.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_NOT_PENDING", "대기 중인 가입 신청만 승인할 수 있습니다.");
        }
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

    // AI작성: 기존 프로젝트의 생성자 주입 방식을 사용한다.
    public SavingGroupServiceImpl(SavingGroupMapper savingGroupMapper) {
        this.savingGroupMapper = savingGroupMapper;
    }

    // AI작성: 모임장을 포함한 승인 회원을 일괄 집계하고 빈 결과는 정상 반환한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition request) {
        String regionName = request.getRegionCode() == null ? null
                : REGIONS.get(Integer.parseInt(request.getRegionCode()) - 1);
        SavingGroupListCondition condition = new SavingGroupListCondition(
                escapeKeyword(request.getKeyword()), regionName,
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
                new SavingGroupSummaryResponse(row.getSavingGroupId(), row.getGroupName(),
                        row.getDescription(), row.getRegionName(), row.getMaxMembers(),
                        counts.getOrDefault(row.getGroupId(), 0L), row.getDuesAmount(),
                        row.getDuesDay(), row.getStatus())).toList();
        return new SavingGroupListResponse(items, pageMeta);
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
        if (member == null || !ACTIVE_ACCOUNT_STATUS.equals(member.getAccountStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_ACCOUNT_UNAVAILABLE",
                    "현재 계정 상태에서는 가입 신청할 수 없습니다.");
        }
        if (member.getIdentityVerifiedAt() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "IDENTITY_VERIFICATION_REQUIRED",
                    "본인 인증 완료 후 가입 신청할 수 있습니다.");
        }

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

        long approvedMemberCount = savingGroupMapper.selectApprovedMemberCountForUpdate(target.getGroupId());
        if (approvedMemberCount >= target.getMaxMembers()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_FULL", "계모임 정원이 모두 찼습니다.");
        }

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
    }

    private boolean isLeftMembership(SavingGroupMembershipRelationRow relation) {
        return relation != null && "LEFT".equals(relation.getMembershipStatus());
    }

    private void validateExistingMembership(SavingGroupMembershipRelationRow relation) {
        if (relation == null || "LEFT".equals(relation.getMembershipStatus())) {
            return;
        }
        if ("APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_JOINED_SAVING_GROUP", "이미 가입한 계모임입니다.");
        }
        if ("PENDING".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_ALREADY_PENDING",
                    "이미 가입 신청한 계모임입니다.");
        }
        if ("REJECTED".equals(relation.getMembershipStatus())
                || "EXPELLED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REAPPLICATION_FORBIDDEN",
                    "재신청할 수 없는 계모임입니다.");
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
