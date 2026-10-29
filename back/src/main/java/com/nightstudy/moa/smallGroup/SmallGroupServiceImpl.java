package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinMemberRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsAuthorityRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsBasicUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsDetailUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 목록 조건·페이징·승인 계원 수 조합을 담당한다.
@Service
public class SmallGroupServiceImpl implements SmallGroupService {
    private static final int PAGE_SIZE = 6;
    private static final int JOIN_REQUEST_PAGE_SIZE = 10;
    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SmallGroupMapper smallGroupMapper;

    public SmallGroupServiceImpl(SmallGroupMapper smallGroupMapper) {
        this.smallGroupMapper = smallGroupMapper;
    }

    // AI작성: 목록 조회와 권한 검증을 같은 트랜잭션에서 처리해 개설자·OWNER 관계가 바뀐 뒤 목록을 반환하지 않는다.
    @Override
    @Transactional
    public SmallGroupJoinRequestListResponse getSmallGroupJoinRequests(
            Long smallGroupId, Long memberId, String status, int page) {
        if (smallGroupId == null || smallGroupId <= 0 || page < 1
                || (status != null && !List.of("PENDING", "APPROVED", "REJECTED").contains(status))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                    "소모임 번호, 신청 상태 또는 페이지가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        if (!Objects.equals(memberId, target.getCreatorMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OWNER_REQUIRED",
                    "소모임 개설자만 가입 신청을 조회할 수 있습니다.");
        }

        SmallGroupMemberRelationRow owner =
                smallGroupMapper.selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (owner == null || !"OWNER".equals(owner.getMemberRole())
                || !"APPROVED".equals(owner.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OWNER_REQUIRED",
                    "승인된 소모임 개설자만 가입 신청을 조회할 수 있습니다.");
        }

        int total = smallGroupMapper.selectSmallGroupJoinRequestCount(target.getGroupId(), status);
        PageMeta pageMeta = PageMeta.of(page, JOIN_REQUEST_PAGE_SIZE, total);
        // AI작성: 빈 결과와 범위 초과는 목록 SQL을 실행하지 않고 요청한 페이지 메타를 그대로 반환한다.
        List<SmallGroupJoinRequestRow> items = page > pageMeta.getTotalPages()
                ? List.of()
                : smallGroupMapper.selectSmallGroupJoinRequests(
                        target.getGroupId(), status, pageMeta.getOffset(), JOIN_REQUEST_PAGE_SIZE);
        return new SmallGroupJoinRequestListResponse(items, pageMeta);
    }

    // AI작성: 공개 상세는 비회원도 반환하고, 관계 데이터가 있는 로그인 사용자만 역할을 판단한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupDetailResponse getSmallGroupDetail(Long smallGroupId, Long memberId) {
        SmallGroupDetailRow row = smallGroupMapper.selectSmallGroupDetail(smallGroupId);
        if (row == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 소모임이 없습니다.");
        }

        String myRole = memberId == null ? null : resolveMemberRole(row.getGroupId(), memberId);
        long memberCount = smallGroupMapper.selectApprovedSmallGroupMemberCount(row.getGroupId());
        // AI작성: 비회원도 제목·작성자·작성일을 볼 수 있도록 역할과 무관하게 공지 요약을 함께 조회한다.
        List<SmallGroupNoticeSummaryResponse> notices =
                smallGroupMapper.selectRecentSmallGroupNotices(row.getGroupId());

        return new SmallGroupDetailResponse(
                row.getSmallGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getLikeCategory(), row.getMeetDate(), row.getLocation(),
                row.getMaxMembers(), memberCount, row.getCreatorNickname(), row.getCreatedAt(), myRole,
                notices);
    }

    // AI작성: 소모임에는 상태별 설정 제한이 없으므로 삭제 여부와 개설자·승인 OWNER 권한만 확인한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupSettingsResponse getSmallGroupSettings(Long smallGroupId, Long loginMemberId) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupSettingsAuthorityRow authority =
                smallGroupMapper.selectSmallGroupSettingsAuthority(smallGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임 정보를 찾을 수 없습니다.");
        }

        boolean isOwner = Objects.equals(authority.getGroupCreatorMemberId(), loginMemberId)
                && Objects.equals(authority.getOwnerMemberId(), loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_SETTINGS_FORBIDDEN",
                    "개설자만 설정을 조회할 수 있습니다.");
        }

        SmallGroupSettingsResponse settings =
                smallGroupMapper.selectSmallGroupCurrentSettings(smallGroupId);
        if (settings == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임 정보를 찾을 수 없습니다.");
        }
        return settings;
    }

    // AI작성: 개설자·승인 OWNER·승인 참여 인원을 모두 잠근 뒤 두 테이블의 전달된 필드만 한 트랜잭션으로 수정한다.
    @Override
    @Transactional
    public void updateSmallGroupSettings(
            Long smallGroupId, Long loginMemberId, SmallGroupSettingsPatchRequest request) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupSettingsAuthorityRow authority =
                smallGroupMapper.selectSmallGroupSettingsAuthorityForUpdate(smallGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임 정보를 찾을 수 없습니다.");
        }

        Long ownerMemberId = smallGroupMapper.selectSmallGroupOwnerMemberIdForUpdate(authority.getGroupId());
        boolean isOwner = Objects.equals(authority.getGroupCreatorMemberId(), loginMemberId)
                && Objects.equals(ownerMemberId, loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_SETTINGS_FORBIDDEN",
                    "개설자만 설정을 변경할 수 있습니다.");
        }

        normalizeSettings(request);

        if (hasBasicSettingsUpdate(request)) {
            SmallGroupSettingsBasicUpdate basicUpdate = createBasicSettingsUpdate(authority.getGroupId(), request);
            if (basicUpdate.getMaxMembers() != null) {
                long approvedMemberCount =
                        smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(authority.getGroupId());
                if (basicUpdate.getMaxMembers() < approvedMemberCount) {
                    throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_BELOW_MEMBERS",
                            "승인된 참여자 수보다 작게 설정할 수 없습니다.");
                }
            }
            smallGroupMapper.updateSmallGroupBasicSettings(basicUpdate);
        }

        if (hasDetailSettingsUpdate(request)) {
            smallGroupMapper.updateSmallGroupDetailSettings(
                    createDetailSettingsUpdate(smallGroupId, request));
        }
    }

    // AI작성: 회원·소모임·기존 관계를 같은 트랜잭션에서 잠가 역할·중복·정원 검증 뒤에만 대기 신청을 저장한다.
    @Override
    @Transactional
    public void requestSmallGroupJoin(Long smallGroupId, Long memberId) {
        if (smallGroupId == null || smallGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupJoinMemberRow member = smallGroupMapper.selectSmallGroupJoinMemberForUpdate(memberId);
        if (member == null || !ACTIVE_ACCOUNT_STATUS.equals(member.getAccountStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_ACCOUNT_UNAVAILABLE",
                    "현재 계정 상태에서는 가입 신청할 수 없습니다.");
        }
        if (member.getIdentityVerifiedAt() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "IDENTITY_VERIFICATION_REQUIRED",
                    "본인 인증 완료 후 가입 신청할 수 있습니다.");
        }
        if ("ADMIN".equals(member.getSystemRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_JOIN_ADMIN_NOT_ALLOWED",
                    "관리자 계정은 소모임 가입 신청을 할 수 없습니다.");
        }

        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        if (target.getMaxMembers() == null || target.getMaxMembers() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_NOT_CONFIGURED",
                    "소모임 정원이 설정되지 않았습니다.");
        }
        if (Objects.equals(target.getCreatorMemberId(), memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED",
                    "개설자 또는 운영진은 자신의 소모임에 가입 신청할 수 없습니다.");
        }

        SmallGroupMemberRelationRow relation = smallGroupMapper
                .selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        validateSmallGroupJoinRole(relation);
        boolean isReapplication = isLeftMembership(relation);
        validateExistingMembership(relation);

        long approvedMemberCount = smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(target.getGroupId());
        if (approvedMemberCount >= target.getMaxMembers()) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_FULL", "소모임 정원이 모두 찼습니다.");
        }

        // TODO: 소모임 모집 상태·공개 범위·자동 승인 여부가 확정되면 대상 잠금 조회 결과를 기준으로 가입 가능 조건을 추가한다.
        try {
            int affectedRows = isReapplication
                    ? smallGroupMapper.reactivateLeftSmallGroupMembership(target.getGroupId(), memberId)
                    : smallGroupMapper.insertSmallGroupJoinRequest(target.getGroupId(), memberId);
            if (affectedRows != 1) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "JOIN_REQUEST_CREATE_FAILED",
                        "가입 신청 처리에 실패했습니다.");
            }
        } catch (DuplicateKeyException e) {
            // AI작성: 유니크 제약 충돌은 서버 오류가 아닌 동일 소모임의 중복 신청으로 응답한다.
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_DUPLICATED",
                    "이미 가입 신청한 소모임입니다.");
        }
    }

    // AI작성: 개설자 외에도 OWNER·MANAGER 관계는 상태와 무관하게 자기 소모임 신청을 허용하지 않는다.
    private void validateSmallGroupJoinRole(SmallGroupMemberRelationRow relation) {
        if (relation == null) {
            return;
        }
        if ("OWNER".equals(relation.getMemberRole()) || "MANAGER".equals(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED",
                    "개설자 또는 운영진은 자신의 소모임에 가입 신청할 수 없습니다.");
        }
        if (!"MEMBER".equals(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBERSHIP_ROLE_INVALID",
                    "가입 역할 정보를 처리할 수 없습니다.");
        }
    }

    private boolean isLeftMembership(SmallGroupMemberRelationRow relation) {
        return relation != null && "LEFT".equals(relation.getMembershipStatus());
    }

    private void validateExistingMembership(SmallGroupMemberRelationRow relation) {
        if (relation == null || "LEFT".equals(relation.getMembershipStatus())) {
            return;
        }
        if ("APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_JOINED_SMALL_GROUP", "이미 가입한 소모임입니다.");
        }
        if ("PENDING".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_ALREADY_PENDING",
                    "이미 가입 신청한 소모임입니다.");
        }
        if ("REJECTED".equals(relation.getMembershipStatus())
                || "EXPELLED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REAPPLICATION_FORBIDDEN",
                    "재신청할 수 없는 소모임입니다.");
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBERSHIP_STATUS_INVALID",
                "가입 상태 정보를 처리할 수 없습니다.");
    }

    private void normalizeSettings(SmallGroupSettingsPatchRequest request) {
        if (request.isGroupNameProvided()) {
            request.setGroupName(request.getGroupName().strip());
        }
        if (request.isLikeCategoryProvided()) {
            request.setLikeCategory(request.getLikeCategory().strip());
        }
        if (request.isDescriptionProvided()) {
            request.setDescription(request.getDescription().strip());
        }
        if (request.isLocationProvided()) {
            request.setLocation(request.getLocation().strip());
        }
    }

    private boolean hasBasicSettingsUpdate(SmallGroupSettingsPatchRequest request) {
        return request.isGroupNameProvided()
                || request.isDescriptionProvided()
                || request.isMaxMembersProvided();
    }

    private SmallGroupSettingsBasicUpdate createBasicSettingsUpdate(
            Long groupId, SmallGroupSettingsPatchRequest request) {
        return new SmallGroupSettingsBasicUpdate(
                groupId,
                request.isGroupNameProvided() ? request.getGroupName() : null,
                request.isDescriptionProvided() ? request.getDescription() : null,
                request.isMaxMembersProvided() ? request.getMaxMembers() : null);
    }

    private boolean hasDetailSettingsUpdate(SmallGroupSettingsPatchRequest request) {
        return request.isLikeCategoryProvided()
                || request.isMeetDateProvided()
                || request.isLocationProvided();
    }

    private SmallGroupSettingsDetailUpdate createDetailSettingsUpdate(
            Long smallGroupId, SmallGroupSettingsPatchRequest request) {
        return new SmallGroupSettingsDetailUpdate(
                smallGroupId,
                request.isLikeCategoryProvided() ? request.getLikeCategory() : null,
                request.isMeetDateProvided() ? request.getMeetDate() : null,
                request.isLocationProvided() ? request.getLocation() : null);
    }

    // AI작성: MANAGER는 상태와 무관한 오류 데이터이며, 중복·미정의 역할은 권한을 임의로 선택하지 않는다.
    private String resolveMemberRole(Long groupId, Long memberId) {
        List<SmallGroupMemberRelationRow> relations =
                smallGroupMapper.selectSmallGroupMemberRelations(groupId, memberId);

        if (relations.stream().anyMatch(relation -> "MANAGER".equals(relation.getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");
        }
        if (relations.size() > 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
        if (relations.isEmpty()) {
            return null;
        }

        SmallGroupMemberRelationRow relation = relations.get(0);
        if (!List.of("OWNER", "MEMBER").contains(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
        return "APPROVED".equals(relation.getMembershipStatus()) ? relation.getMemberRole() : null;
    }

    // AI작성: 계모임과 동일한 지역 코드 기준으로 변환하고, 카테고리는 소모임 전용 컬럼으로만 필터링한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupListResponse getSmallGroupList(SmallGroupSearchCondition request) {
        String regionName = request.getRegionCode() == null ? null
                : REGIONS.get(Integer.parseInt(request.getRegionCode()) - 1);
        SmallGroupListCondition condition = new SmallGroupListCondition(
                escapeKeyword(request.getKeyword()), regionName, request.getLikeCategory(), 0, PAGE_SIZE);
        int total = smallGroupMapper.selectSmallGroupCount(condition);
        PageMeta pageMeta = PageMeta.of(request.getPage(), PAGE_SIZE, total);
        // AI작성: 계모임 목록과 동일하게 범위 초과는 offset 계산 전에 빈 목록으로 반환한다.
        if (pageMeta.getPage() > pageMeta.getTotalPages()) {
            return new SmallGroupListResponse(List.of(), pageMeta);
        }
        condition.setOffset(pageMeta.getOffset());
        List<SmallGroupRow> rows = smallGroupMapper.selectSmallGroupList(condition);
        if (rows.isEmpty()) {
            return new SmallGroupListResponse(List.of(), pageMeta);
        }

        Map<Long, Long> memberCounts = smallGroupMapper.selectApprovedMemberCounts(
                        rows.stream().map(SmallGroupRow::getGroupId).distinct().toList())
                .stream().collect(Collectors.toMap(
                        SmallGroupMemberCount::getGroupId, SmallGroupMemberCount::getMemberCount));

        List<SmallGroupSummaryResponse> items = rows.stream().map(row -> new SmallGroupSummaryResponse(
                row.getSmallGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getMaxMembers(), memberCounts.getOrDefault(row.getGroupId(), 0L),
                row.getLikeCategory(), row.getMeetDate(), row.getLocation())).toList();
        return new SmallGroupListResponse(items, pageMeta);
    }

    // AI작성: 계모임 목록과 동일하게 LIKE 와일드카드를 일반 검색 문자로 처리한다.
    private String escapeKeyword(String keyword) {
        return keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
