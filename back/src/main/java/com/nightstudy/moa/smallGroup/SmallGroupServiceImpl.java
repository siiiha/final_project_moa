package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinApprovalTargetRow;
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
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateCommand;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListTargetRow;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
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
    private static final int VOTE_TITLE_MIN_LENGTH = 2;
    private static final int VOTE_TITLE_MAX_LENGTH = 12;
    private static final int VOTE_OPTION_MIN_COUNT = 2;
    private static final int VOTE_OPTION_MAX_COUNT = 8;
    private static final int VOTE_OPTION_MIN_LENGTH = 2;
    private static final int VOTE_OPTION_MAX_LENGTH = 12;
    private static final int MAX_OPEN_VOTE_COUNT = 5;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SmallGroupMapper smallGroupMapper;

    // AI작성: 승인과 거절이 공유하는 잠금 검증 결과를 묶어 처리별 후속 검증을 분리한다.
    private record SmallGroupJoinDecisionTarget(
            SmallGroupJoinApprovalTargetRow request, SmallGroupJoinTargetRow target) {
    }

    public SmallGroupServiceImpl(SmallGroupMapper smallGroupMapper) {
        this.smallGroupMapper = smallGroupMapper;
    }

    // AI작성: 목록 조회는 삭제되지 않은 소모임 확인 후 승인된 OWNER·MEMBER 관계만 허용하고, 검색·필터·페이징 없이 반환한다.
    @Override
    @Transactional(readOnly = true)
    public List<SmallGroupVoteListItemResponse> getSmallGroupVotes(Long smallGroupId, Long memberId) {
        if (smallGroupId == null || smallGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupVoteListTargetRow target = smallGroupMapper.selectSmallGroupVoteListTarget(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        validateSmallGroupVoteListAuthority(target.getGroupId(), memberId);
        return smallGroupMapper.selectSmallGroupVoteList(smallGroupId);
    }

    // AI작성: 탈퇴·대기·추방 관계와 OWNER·MEMBER 이외의 역할은 투표 목록을 볼 수 없다.
    private void validateSmallGroupVoteListAuthority(Long groupId, Long memberId) {
        List<SmallGroupMemberRelationRow> relations =
                smallGroupMapper.selectSmallGroupMemberRelations(groupId, memberId);
        boolean isApprovedMember = relations.size() == 1
                && "APPROVED".equals(relations.get(0).getMembershipStatus())
                && List.of("OWNER", "MEMBER").contains(relations.get(0).getMemberRole());
        if (!isApprovedMember) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_LIST_FORBIDDEN",
                    "승인된 소모임 회원만 투표 목록을 조회할 수 있습니다.");
        }
    }

    // AI작성: 소모임·개설자·승인 OWNER 관계를 잠근 뒤 검증된 투표와 보기를 한 트랜잭션으로 저장한다.
    @Override
    @Transactional
    public SmallGroupVoteCreateResponse createSmallGroupVote(
            Long smallGroupId, Long memberId, SmallGroupVoteCreateRequest request) {
        if (smallGroupId == null || smallGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        validateSmallGroupVoteCreateAuthority(target, memberId);

        SmallGroupVoteCreateCommand command = createSmallGroupVoteCommand(smallGroupId, memberId, request);
        if (smallGroupMapper.selectOpenSmallGroupVoteCountForUpdate(smallGroupId) >= MAX_OPEN_VOTE_COUNT) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_OPEN_VOTE_LIMIT_EXCEEDED",
                    "진행 중인 투표는 최대 5개까지 생성할 수 있습니다.");
        }
        if (smallGroupMapper.insertSmallGroupVote(command) != 1 || command.getSmallGroupVoteId() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_VOTE_CREATE_FAILED",
                    "투표 생성 처리에 실패했습니다.");
        }
        if (smallGroupMapper.insertSmallGroupVoteOptions(
                command.getSmallGroupVoteId(), command.getOptions()) != command.getOptions().size()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_VOTE_OPTION_CREATE_FAILED",
                    "투표 보기 저장에 실패했습니다.");
        }

        // TODO: 알림 기능 담당 구현 후, 승인된 소모임 회원에게 투표 생성 알림을 발송한다.
        // TODO: 투표 종료 기능 담당 구현 후, 마감·생성자 종료 시 종료 알림을 발송한다.
        return new SmallGroupVoteCreateResponse(command.getSmallGroupVoteId());
    }

    // AI작성: 투표 생성 권한은 groups 개설자이면서 승인된 OWNER 관계인 회원으로만 한정한다.
    private void validateSmallGroupVoteCreateAuthority(SmallGroupJoinTargetRow target, Long memberId) {
        if (!Objects.equals(memberId, target.getCreatorMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_CREATE_FORBIDDEN",
                    "소모임 개설자만 투표를 생성할 수 있습니다.");
        }
        SmallGroupMemberRelationRow relation =
                smallGroupMapper.selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (relation == null || !"OWNER".equals(relation.getMemberRole())
                || !"APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_CREATE_FORBIDDEN",
                    "승인된 소모임 개설자만 투표를 생성할 수 있습니다.");
        }
    }

    // AI작성: 공백·빈 값·완전히 같은 보기를 정리한 뒤, 확정된 제목·보기·마감일 정책을 검증한다.
    private SmallGroupVoteCreateCommand createSmallGroupVoteCommand(
            Long smallGroupId, Long memberId, SmallGroupVoteCreateRequest request) {
        if (request == null || request.getMultipleChoice() == null || request.getAnonymous() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "투표 생성 요청이 올바르지 않습니다.");
        }

        String title = normalizeVoteTitle(request.getTitle());
        List<String> options = normalizeVoteOptions(request.getOptions());
        validateVoteEndsAt(request.getEndsAt());
        return new SmallGroupVoteCreateCommand(
                null,
                smallGroupId,
                memberId,
                title,
                request.getMultipleChoice(),
                request.getAnonymous(),
                request.getEndsAt(),
                options);
    }

    private String normalizeVoteTitle(String title) {
        if (title == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "투표 제목을 입력해 주세요.");
        }
        String normalizedTitle = title.strip();
        if (normalizedTitle.length() < VOTE_TITLE_MIN_LENGTH || normalizedTitle.length() > VOTE_TITLE_MAX_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                    "투표 제목은 공백을 제외하고 2자 이상 12자 이하여야 합니다.");
        }
        return normalizedTitle;
    }

    private List<String> normalizeVoteOptions(List<String> rawOptions) {
        if (rawOptions == null || rawOptions.size() > VOTE_OPTION_MAX_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "투표 보기는 최대 8개까지 입력할 수 있습니다.");
        }

        LinkedHashSet<String> normalizedOptions = new LinkedHashSet<>();
        for (String option : rawOptions) {
            if (option == null || option.strip().isEmpty()) {
                continue;
            }
            String normalizedOption = option.strip();
            if (normalizedOption.length() < VOTE_OPTION_MIN_LENGTH
                    || normalizedOption.length() > VOTE_OPTION_MAX_LENGTH) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                        "투표 보기는 공백을 제외하고 2자 이상 12자 이하여야 합니다.");
            }
            normalizedOptions.add(normalizedOption);
        }
        if (normalizedOptions.size() < VOTE_OPTION_MIN_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                    "빈 값과 중복을 제외한 투표 보기는 2개 이상이어야 합니다.");
        }
        return List.copyOf(normalizedOptions);
    }

    private void validateVoteEndsAt(LocalDateTime endsAt) {
        if (endsAt == null) {
            return;
        }
        LocalDateTime minimumEndsAt = LocalDateTime.now(KOREA_ZONE_ID).plusMinutes(10);
        if (endsAt.isBefore(minimumEndsAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                    "투표 마감 일시는 현재 시각 기준 10분 이후부터 설정할 수 있습니다.");
        }
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
        validateSmallGroupOwner(target, memberId, "조회");

        int total = smallGroupMapper.selectSmallGroupJoinRequestCount(target.getGroupId(), status);
        PageMeta pageMeta = PageMeta.of(page, JOIN_REQUEST_PAGE_SIZE, total);
        // AI작성: 빈 결과와 범위 초과는 목록 SQL을 실행하지 않고 요청한 페이지 메타를 그대로 반환한다.
        List<SmallGroupJoinRequestRow> items = page > pageMeta.getTotalPages()
                ? List.of()
                : smallGroupMapper.selectSmallGroupJoinRequests(
                        target.getGroupId(), status, pageMeta.getOffset(), JOIN_REQUEST_PAGE_SIZE);
        return new SmallGroupJoinRequestListResponse(items, pageMeta);
    }

    // AI작성: 신청 회원 → 소모임 → 신청 행 순서로 잠가 신청 소속·개설자 권한·대기 상태·정원을 재검증한다.
    @Override
    @Transactional
    public void approveSmallGroupJoin(Long smallGroupId, Long crewMemberId, Long memberId) {
        SmallGroupJoinDecisionTarget decision = validatePendingSmallGroupJoinRequest(
                smallGroupId, crewMemberId, memberId, "승인");
        SmallGroupJoinApprovalTargetRow request = decision.request();
        SmallGroupJoinTargetRow target = decision.target();
        if (target.getMaxMembers() == null || target.getMaxMembers() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_NOT_CONFIGURED",
                    "소모임 정원이 설정되지 않았습니다.");
        }
        if (smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(target.getGroupId())
                >= target.getMaxMembers()) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_FULL", "소모임 정원이 모두 찼습니다.");
        }

        int affectedRows = smallGroupMapper.approveSmallGroupJoinRequest(
                request.getCrewMemberId(), request.getGroupId(), memberId);
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_APPROVAL_FAILED",
                    "가입 신청 상태가 변경되어 승인할 수 없습니다. 다시 확인해주세요.");
        }
    }

    // AI작성: 거절은 승인과 달리 정원·회원 관계를 변경하지 않고 대기 신청 상태만 REJECTED로 전환한다.
    @Override
    @Transactional
    public void rejectSmallGroupJoin(Long smallGroupId, Long crewMemberId, Long memberId) {
        SmallGroupJoinDecisionTarget decision = validatePendingSmallGroupJoinRequest(
                smallGroupId, crewMemberId, memberId, "거절");
        SmallGroupJoinApprovalTargetRow request = decision.request();
        int affectedRows = smallGroupMapper.rejectSmallGroupJoinRequest(
                request.getCrewMemberId(), request.getGroupId());
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_REJECTION_FAILED",
                    "가입 신청 상태가 변경되어 거절할 수 없습니다. 다시 확인해주세요.");
        }
    }

    // AI작성: 신청 회원 → 소모임 → 신청 행 순서로 잠가 승인·거절의 공통 권한·소속·대기 상태를 재검증한다.
    private SmallGroupJoinDecisionTarget validatePendingSmallGroupJoinRequest(
            Long smallGroupId, Long crewMemberId, Long memberId, String actionName) {
        if (smallGroupId == null || smallGroupId <= 0 || crewMemberId == null || crewMemberId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 또는 가입 신청 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupJoinApprovalTargetRow identified =
                smallGroupMapper.selectSmallGroupJoinApprovalTarget(crewMemberId);
        if (identified == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "가입 신청을 찾을 수 없습니다.");
        }
        SmallGroupJoinMemberRow applicant =
                smallGroupMapper.selectSmallGroupJoinMemberForUpdate(identified.getMemberId());
        if (applicant == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_MEMBER_NOT_FOUND", "신청 회원을 찾을 수 없습니다.");
        }
        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        SmallGroupJoinApprovalTargetRow request =
                smallGroupMapper.selectSmallGroupJoinApprovalTargetForUpdate(crewMemberId);
        if (request == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "가입 신청을 찾을 수 없습니다.");
        }
        if (!Objects.equals(request.getMemberId(), identified.getMemberId())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_CHANGED",
                    "가입 신청이 변경되었습니다. 다시 확인해주세요.");
        }
        if (!Objects.equals(request.getGroupId(), target.getGroupId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND",
                    "해당 소모임의 가입 신청을 찾을 수 없습니다.");
        }
        validateSmallGroupOwner(target, memberId, actionName);
        if (!"PENDING".equals(request.getMembershipStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_NOT_PENDING",
                    "대기 중인 가입 신청만 " + actionName + "할 수 있습니다.");
        }
        return new SmallGroupJoinDecisionTarget(request, target);
    }

    // AI작성: 목록 조회와 승인에서 groups 생성자와 승인된 OWNER 관계를 함께 확인한다.
    private void validateSmallGroupOwner(
            SmallGroupJoinTargetRow target, Long memberId, String actionName) {
        if (!Objects.equals(memberId, target.getCreatorMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OWNER_REQUIRED",
                    "소모임 개설자만 가입 신청을 " + actionName + "할 수 있습니다.");
        }
        SmallGroupMemberRelationRow owner =
                smallGroupMapper.selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (owner == null || !"OWNER".equals(owner.getMemberRole())
                || !"APPROVED".equals(owner.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OWNER_REQUIRED",
                    "승인된 소모임 개설자만 가입 신청을 " + actionName + "할 수 있습니다.");
        }
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
