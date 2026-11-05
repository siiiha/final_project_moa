package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
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
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteMyParticipationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionVoterNicknameRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteParticipationRequest;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateCommand;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewSearchCondition;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    // AI작성: 동일 모임의 고정 변경을 모임 잠금으로 직렬화해 동시 요청에서도 최대 두 개를 지킨다.
    @Override @Transactional
    public void updateSmallGroupNoticePinned(Long smallGroupId, Long noticeId, Long memberId, Boolean pinned) {
        var target = requireSmallGroupNoticeOwner(smallGroupId, memberId);
        if (noticeId == null || noticeId <= 0 || pinned == null) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "공지 번호와 고정 여부를 확인해 주세요.");
        var notice = smallGroupMapper.selectSmallGroupNoticePinForUpdate(target.getGroupId(), noticeId);
        if (notice == null) throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        if (notice.getPinned() == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "공지 상태를 확인하지 못했습니다.");
        // AI작성: 같은 상태의 요청은 성공으로 처리하고 고정 해제에는 개수 제한을 적용하지 않는다.
        if (pinned.equals(notice.getPinned())) return;
        if (pinned && smallGroupMapper.selectSmallGroupPinnedNoticeIdsForUpdate(target.getGroupId()).size() >= 2) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED", "고정 공지는 최대 2개까지 지정할 수 있습니다.");
        }
        if (smallGroupMapper.updateSmallGroupNoticePinned(target.getGroupId(), noticeId, pinned) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_NOTICE_PIN_FAILED", "공지 고정 변경에 실패했습니다.");
        }
    }

    // AI작성: 수정과 동일한 잠금 순서·작성자 검증을 사용해 본인 공지의 삭제 시각만 변경한다.
    @Override @Transactional
    public void deleteSmallGroupNotice(Long smallGroupId, Long noticeId, Long memberId) {
        var target = requireSmallGroupNoticeOwner(smallGroupId, memberId);
        requireSmallGroupNoticeAuthor(target.getGroupId(), noticeId, memberId);
        if (smallGroupMapper.deleteSmallGroupNotice(target.getGroupId(), noticeId, memberId) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_NOTICE_DELETE_FAILED", "공지 삭제에 실패했습니다.");
        }
    }

    // AI작성: 공통 모임→회원 관계→공지 순으로 잠그고 동일 내용 재요청은 성공 처리한다.
    @Override @Transactional
    public Long updateSmallGroupNotice(Long smallGroupId, Long noticeId, Long memberId, String title, String content) {
        var target = requireSmallGroupNoticeOwner(smallGroupId, memberId);
        validateSmallGroupNoticeText(title, content);
        var notice = requireSmallGroupNoticeAuthor(target.getGroupId(), noticeId, memberId);
        if (Objects.equals(notice.getTitle(), title.trim()) && Objects.equals(notice.getContent(), content.trim())) return noticeId;
        if (smallGroupMapper.updateSmallGroupNotice(target.getGroupId(), noticeId, memberId, title.trim(), content.trim()) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_NOTICE_EDIT_FAILED", "공지 수정에 실패했습니다.");
        }
        return noticeId;
    }

    // AI작성: 현재 소모임장이라도 다른 작성자의 공지를 수정·삭제할 수 없다.
    private com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeDetailResponse requireSmallGroupNoticeAuthor(Long groupId, Long noticeId, Long memberId) {
        if (noticeId == null || noticeId <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "공지 번호가 올바르지 않습니다.");
        var notice = smallGroupMapper.selectSmallGroupNoticeForUpdate(groupId, noticeId);
        if (notice == null) throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        if (!Objects.equals(notice.getWriterId(), memberId)) throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_NOTICE_AUTHOR_FORBIDDEN", "공지 작성자만 수정·삭제할 수 있습니다.");
        return notice;
    }

    // AI작성: 공개 상세와 같은 모임·관계 검증을 먼저 수행하고 승인 회원에게만 본문을 제공한다.
    @Override @Transactional(readOnly = true)
    public com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeDetailResponse getSmallGroupNoticeDetail(Long smallGroupId, Long noticeId, Long memberId) {
        if (memberId == null || memberId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (noticeId == null || noticeId <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "공지 번호가 올바르지 않습니다.");
        SmallGroupDetailResponse group = getSmallGroupDetail(smallGroupId, memberId);
        if (!"APPROVED".equals(group.getMembershipStatus()) || !List.of("OWNER", "MEMBER").contains(group.getMyRole() == null ? "" : group.getMyRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_NOTICE_READ_FORBIDDEN", "해당 소모임의 승인된 회원만 공지를 조회할 수 있습니다.");
        }
        var notice = smallGroupMapper.selectSmallGroupNoticeDetail(group.getGroupId(), noticeId);
        if (notice == null) throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOTICE_NOT_FOUND", "공지를 찾을 수 없습니다.");
        return notice;
    }

    // AI작성: 마지막 페이지 초과 요청은 유효한 마지막 페이지로 보정하며 빈 목록은 1페이지로 유지한다.
    @Override @Transactional(readOnly = true)
    public com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeListResponse getSmallGroupNotices(Long groupId,
            com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSearchCondition condition) {
        if (condition == null || condition.getNoticePage() == null || condition.getNoticePage() < 1
                || !List.of("LATEST", "OLDEST").contains(condition.getNoticeSort() == null ? "" : condition.getNoticeSort())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "공지 조회 조건을 확인해 주세요.");
        }
        int total = smallGroupMapper.countSmallGroupNotices(groupId);
        int page = Math.min(condition.getNoticePage(), Math.max(1, (int)Math.ceil(total / 4.0)));
        PageMeta meta = PageMeta.of(page, 4, total);
        return new com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeListResponse(
                smallGroupMapper.selectSmallGroupNoticePage(groupId, meta.getOffset(), condition.getNoticeSort()), meta,
                smallGroupMapper.selectSmallGroupPinnedNotices(groupId));
    }

    // AI작성: 공통 모임→회원 관계 순으로 잠가 권한 검증과 저장을 한 트랜잭션에서 처리한다.
    @Override
    @Transactional
    public void createSmallGroupNotice(Long smallGroupId, Long memberId, String title, String content) {
        SmallGroupJoinTargetRow target = requireSmallGroupNoticeOwner(smallGroupId, memberId);
        validateSmallGroupNoticeText(title, content);
        var row = new com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeCreateRow(
                null, target.getGroupId(), memberId, title.trim(), content.trim());
        if (smallGroupMapper.insertSmallGroupNotice(row) != 1 || row.getNoticeId() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_NOTICE_WRITE_FAILED", "공지 등록에 실패했습니다.");
        }
    }

    // AI작성: 소모임 ID와 groups ID를 구분하며 개설자·승인 OWNER를 동시에 확인한다.
    private SmallGroupJoinTargetRow requireSmallGroupNoticeOwner(Long smallGroupId, Long memberId) {
        if (memberId == null || memberId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (smallGroupId == null || smallGroupId <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        SmallGroupMemberRelationRow relation = smallGroupMapper.selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (!Objects.equals(target.getCreatorMemberId(), memberId) || relation == null
                || !"OWNER".equals(relation.getMemberRole()) || !"APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_NOTICE_WRITE_FORBIDDEN", "승인된 해당 소모임장만 공지를 관리할 수 있습니다.");
        }
        return target;
    }

    // AI작성: Controller 외 호출에도 계모임과 같은 제목 100자·본문 5,000자 규칙을 적용한다.
    private void validateSmallGroupNoticeText(String title, String content) {
        if (title == null || title.isBlank() || title.length() > 100 || content == null || content.isBlank() || content.length() > 5000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "제목은 1~100자, 내용은 1~5,000자로 입력해 주세요.");
        }
    }

    private static final int PAGE_SIZE = 6;
    private static final int JOIN_REQUEST_PAGE_SIZE = 10;
    private static final int VOTE_TITLE_MIN_LENGTH = 2;
    private static final int VOTE_TITLE_MAX_LENGTH = 12;
    private static final int VOTE_OPTION_MIN_COUNT = 2;
    private static final int VOTE_OPTION_MAX_COUNT = 8;
    private static final int VOTE_OPTION_MIN_LENGTH = 2;
    private static final int VOTE_OPTION_MAX_LENGTH = 12;
    private static final int MAX_OPEN_VOTE_COUNT = 5;
    private static final long VOTE_CLOSED_VISIBLE_HOURS = 24L;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SmallGroupMapper smallGroupMapper;
    private final NotificationService notificationService;

    // AI작성: 승인과 거절이 공유하는 잠금 검증 결과를 묶어 처리별 후속 검증을 분리한다.
    private record SmallGroupJoinDecisionTarget(
            SmallGroupJoinApprovalTargetRow request, SmallGroupJoinTargetRow target) {
    }

    public SmallGroupServiceImpl(
            SmallGroupMapper smallGroupMapper,
            NotificationService notificationService
    ) {
        this.smallGroupMapper = smallGroupMapper;
        this.notificationService = notificationService;
    }

    // AI작성: 목록 페이지와 무관하게 본인의 기존 후기 ID를 조회해 버튼 표시 여부를 판단한다.
    @Override
    @Transactional(readOnly = true)
    public boolean hasWrittenSmallGroupReview(Long smallGroupId, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (smallGroupId == null || smallGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        }
        SmallGroupDetailRow target = smallGroupMapper.selectSmallGroupDetail(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 소모임이 없습니다.");
        }
        resolveMemberRole(target.getGroupId(), memberId);
        return smallGroupMapper.selectSmallGroupReviewId(smallGroupId, memberId) != null;
    }

    // AI작성: 공개 상세와 같은 소모임·관계 조회를 재사용하며 등록 권한이나 모임 날짜로 목록을 제한하지 않는다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupReviewListResponse getSmallGroupReviews(
            Long smallGroupId, Long memberId, SmallGroupReviewSearchCondition request) {
        if (smallGroupId == null || smallGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        }
        if (request == null || request.getPage() == null || request.getPage() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "페이지 번호는 1 이상이어야 합니다.");
        }
        if (memberId != null && memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupDetailRow target = smallGroupMapper.selectSmallGroupDetail(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 소모임이 없습니다.");
        }
        // AI작성: 승인 여부는 조회 조건으로 쓰지 않고 MANAGER·중복·미정의 역할만 기존 상세 정책으로 검증한다.
        if (memberId != null) {
            resolveMemberRole(target.getGroupId(), memberId);
        }

        SmallGroupReviewListCondition condition = new SmallGroupReviewListCondition(smallGroupId, 0);
        int total = smallGroupMapper.selectSmallGroupReviewCount(condition);
        PageMeta pageMeta = PageMeta.of(request.getPage(), condition.getLimit(), total);
        // AI작성: 빈 결과·범위 초과는 offset 계산 전에 반환해 큰 페이지 번호의 정수 오버플로도 방지한다.
        if (pageMeta.getPage() > pageMeta.getTotalPages()) {
            return new SmallGroupReviewListResponse(List.of(), pageMeta);
        }
        condition.setOffset(pageMeta.getOffset());
        return new SmallGroupReviewListResponse(smallGroupMapper.selectSmallGroupReviewList(condition), pageMeta);
    }

    // AI작성: 소모임·회원 관계를 잠가 권한·날짜·중복을 검증한 뒤 같은 트랜잭션에서 저장한다.
    @Override
    @Transactional
    public Long createSmallGroupReview(
            Long smallGroupId, Long memberId, SmallGroupReviewCreateRequest request) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (smallGroupId == null || smallGroupId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다.");
        }
        validateSmallGroupReviewRequest(request);

        SmallGroupReviewCreateTargetRow target =
                smallGroupMapper.selectSmallGroupReviewCreateTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        if (Objects.equals(memberId, target.getCreatorMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_REVIEW_OWNER_NOT_ALLOWED",
                    "소모임 개설자는 후기를 등록할 수 없습니다.");
        }
        SmallGroupMemberRelationRow relation =
                smallGroupMapper.selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        // AI작성: MANAGER는 가입 상태와 무관하게 기존 소모임 접근 제한을 그대로 적용한다.
        if (relation != null && "MANAGER".equals(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");
        }
        if (relation == null || !"MEMBER".equals(relation.getMemberRole())
                || !"APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_REVIEW_CREATE_FORBIDDEN",
                    "승인된 소모임 일반 모임원만 후기를 등록할 수 있습니다.");
        }
        // AI작성: 서버 기본 시간대와 무관하게 한국 날짜 기준 모임 다음 날부터 허용한다.
        if (target.getMeetDate() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_REVIEW_MEET_DATE_REQUIRED",
                    "모임 날짜가 설정되지 않아 후기를 등록할 수 없습니다.");
        }
        if (!target.getMeetDate().isBefore(LocalDate.now(KOREA_ZONE_ID))) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_REVIEW_NOT_AVAILABLE_YET",
                    "후기는 모임 다음 날부터 등록할 수 있습니다.");
        }
        if (smallGroupMapper.selectSmallGroupReviewId(smallGroupId, memberId) != null) {
            throw smallGroupReviewAlreadyExists();
        }

        SmallGroupReviewCreateCommand command = new SmallGroupReviewCreateCommand();
        command.setSmallGroupId(smallGroupId);
        command.setWriterId(memberId);
        command.setRating(request.getRating());
        command.setContent(request.getContent());
        int insertedRows;
        try {
            insertedRows = smallGroupMapper.insertSmallGroupReview(command);
        } catch (DuplicateKeyException exception) {
            // AI작성: 사전 조회 이후 발생한 동시 중복 요청도 동일한 업무 오류로 응답한다.
            throw smallGroupReviewAlreadyExists();
        }
        if (insertedRows != 1 || command.getSmallGroupReviewId() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_REVIEW_CREATE_FAILED",
                    "후기 등록에 실패했습니다.");
        }
        return command.getSmallGroupReviewId();
    }

    // AI작성: Service 직접 호출에서도 DB 반올림·문자열 잘림 전에 DTO와 같은 입력 정책을 검증한다.
    private void validateSmallGroupReviewRequest(SmallGroupReviewCreateRequest request) {
        if (request == null || request.getRating() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "별점은 필수입니다.");
        }
        BigDecimal rating = request.getRating();
        if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(new BigDecimal("5.0")) > 0
                || !request.isRatingStepValid()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "별점은 0~5점, 0.5점 단위로 입력해주세요.");
        }
        String content = request.getContent();
        if (content == null || content.isBlank() || content.length() > 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "코멘트는 1자 이상 30자 이하로 입력해주세요.");
        }
    }

    private ApiException smallGroupReviewAlreadyExists() {
        // AI작성: 사전 중복 확인과 DB UNIQUE 위반에 동일한 오류 코드·메시지를 사용한다.
        return new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_REVIEW_ALREADY_EXISTS",
                "이미 이 소모임에 후기를 등록했습니다.");
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
        validateSmallGroupVoteMember(
                target.getGroupId(), memberId, "SMALL_GROUP_VOTE_LIST_FORBIDDEN",
                "승인된 소모임 회원만 투표 목록을 조회할 수 있습니다.");
        return smallGroupMapper.selectSmallGroupVoteList(smallGroupId);
    }

    // AI작성: 탈퇴·대기·추방 관계와 OWNER·MEMBER 이외의 역할은 투표 목록·상세를 볼 수 없다.
    private void validateSmallGroupVoteMember(
            Long groupId, Long memberId, String errorCode, String errorMessage) {
        List<SmallGroupMemberRelationRow> relations =
                smallGroupMapper.selectSmallGroupMemberRelations(groupId, memberId);
        boolean isApprovedMember = relations.size() == 1
                && "APPROVED".equals(relations.get(0).getMembershipStatus())
                && List.of("OWNER", "MEMBER").contains(relations.get(0).getMemberRole());
        if (!isApprovedMember) {
            throw new ApiException(HttpStatus.FORBIDDEN, errorCode, errorMessage);
        }
    }

    // AI작성: 소속·권한·마감 공개 범위를 검증한 뒤 익명 여부와 참여 여부에 맞는 상세 정보만 조합한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupVoteDetailResponse getSmallGroupVoteDetail(
            Long smallGroupId, Long smallGroupVoteId, Long memberId) {
        return getSmallGroupVoteDetailInternal(smallGroupId, smallGroupVoteId, memberId, false);
    }

    // AI작성: 투표 현황은 기존 상세 응답을 재사용하되, 참여 이력이 없는 회원은 결과 집계 전에 차단한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupVoteDetailResponse getSmallGroupVoteStatus(
            Long smallGroupId, Long smallGroupVoteId, Long memberId) {
        return getSmallGroupVoteDetailInternal(smallGroupId, smallGroupVoteId, memberId, true);
    }

    // AI작성: 상세는 투표 전 보기 선택을 위해 승인 회원에게 열고, 현황만 참여 완료 회원으로 추가 제한한다.
    private SmallGroupVoteDetailResponse getSmallGroupVoteDetailInternal(
            Long smallGroupId, Long smallGroupVoteId, Long memberId, boolean participationRequired) {
        if (smallGroupId == null || smallGroupId <= 0 || smallGroupVoteId == null || smallGroupVoteId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 또는 투표 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupVoteListTargetRow target = smallGroupMapper.selectSmallGroupVoteListTarget(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        validateSmallGroupVoteMember(
                target.getGroupId(), memberId, "SMALL_GROUP_VOTE_DETAIL_FORBIDDEN",
                "승인된 소모임 회원만 투표 상세를 조회할 수 있습니다.");

        SmallGroupVoteDetailRow vote =
                smallGroupMapper.selectSmallGroupVoteDetail(smallGroupId, smallGroupVoteId);
        if (vote == null || isVoteClosedBeyondVisiblePeriod(vote, LocalDateTime.now(KOREA_ZONE_ID))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_VOTE_NOT_FOUND", "투표를 찾을 수 없습니다.");
        }

        Long participationId = smallGroupMapper.selectSmallGroupVoteParticipationId(smallGroupVoteId, memberId);
        List<Long> selectedOptionIds = participationId == null
                ? List.of()
                : smallGroupMapper.selectSmallGroupVoteSelectedOptionIds(participationId);
        boolean participated = participationId != null;
        if (participationRequired && !participated) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_STATUS_FORBIDDEN",
                    "투표를 완료한 소모임 회원만 투표 현황을 조회할 수 있습니다.");
        }
        Map<Long, List<String>> voterNicknamesByOptionId = !vote.isAnonymous() && participated
                ? smallGroupMapper.selectSmallGroupVoteOptionVoterNicknames(smallGroupVoteId).stream()
                        .collect(Collectors.groupingBy(
                                SmallGroupVoteOptionVoterNicknameRow::getSmallGroupVoteOptionId,
                                Collectors.mapping(
                                        SmallGroupVoteOptionVoterNicknameRow::getVoterNickname,
                                        Collectors.toList())))
                : Map.of();
        List<SmallGroupVoteOptionDetailResponse> options =
                smallGroupMapper.selectSmallGroupVoteDetailOptions(smallGroupVoteId).stream()
                        .map(option -> toSmallGroupVoteOptionDetailResponse(option, voterNicknamesByOptionId))
                        .toList();

        return new SmallGroupVoteDetailResponse(
                vote.getSmallGroupVoteId(),
                vote.getTitle(),
                vote.getCreatorNickname(),
                vote.isMultipleChoice(),
                vote.isAnonymous(),
                vote.getCreatedAt(),
                vote.getEndsAt(),
                isVoteClosed(vote, LocalDateTime.now(KOREA_ZONE_ID)) ? "CLOSED" : "OPEN",
                smallGroupMapper.selectSmallGroupVoteParticipantCount(smallGroupVoteId),
                new SmallGroupVoteMyParticipationResponse(participated, selectedOptionIds),
                options);
    }

    // AI작성: 소모임·투표·OWNER 관계를 잠근 뒤 투표 기본 행만 논리 삭제해 참여·선택 이력을 보존한다.
    @Override
    @Transactional
    public void deleteSmallGroupVote(Long smallGroupId, Long smallGroupVoteId, Long memberId) {
        if (smallGroupId == null || smallGroupId <= 0 || smallGroupVoteId == null || smallGroupVoteId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 또는 투표 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }

        SmallGroupVoteDetailRow vote =
                smallGroupMapper.selectSmallGroupVoteDetailForUpdate(smallGroupId, smallGroupVoteId);
        if (vote == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_VOTE_NOT_FOUND", "투표를 찾을 수 없습니다.");
        }
        validateSmallGroupVoteDeleteAuthority(target, vote, memberId);

        if (smallGroupMapper.softDeleteSmallGroupVote(smallGroupId, smallGroupVoteId) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_VOTE_DELETE_FAILED",
                    "투표 삭제 처리에 실패했습니다.");
        }
    }

    // AI작성: 투표 작성자 본인과 groups 개설자·승인 OWNER가 모두 같은 회원일 때만 삭제를 허용한다.
    private void validateSmallGroupVoteDeleteAuthority(
            SmallGroupJoinTargetRow target, SmallGroupVoteDetailRow vote, Long memberId) {
        if (!Objects.equals(memberId, vote.getCreatorMemberId())
                || !Objects.equals(memberId, target.getCreatorMemberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_DELETE_FORBIDDEN",
                    "투표 작성자인 소모임 개설자만 투표를 삭제할 수 있습니다.");
        }
        SmallGroupMemberRelationRow relation =
                smallGroupMapper.selectSmallGroupMembershipForUpdate(target.getGroupId(), memberId);
        if (relation == null || !"OWNER".equals(relation.getMemberRole())
                || !"APPROVED".equals(relation.getMembershipStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_DELETE_FORBIDDEN",
                    "승인된 소모임 개설자만 투표를 삭제할 수 있습니다.");
        }
    }

    // AI작성: 소모임·회원·투표를 잠근 상태에서 최초 참여 저장 또는 선택 변경만 원자적으로 처리한다.
    @Override
    @Transactional
    public void participateSmallGroupVote(
            Long smallGroupId, Long smallGroupVoteId, Long memberId,
            SmallGroupVoteParticipationRequest request) {
        if (smallGroupId == null || smallGroupId <= 0 || smallGroupVoteId == null || smallGroupVoteId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 또는 투표 번호가 올바르지 않습니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupJoinTargetRow target = smallGroupMapper.selectSmallGroupJoinTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임을 찾을 수 없습니다.");
        }
        validateSmallGroupVoteMemberForUpdate(target.getGroupId(), memberId);

        SmallGroupVoteDetailRow vote =
                smallGroupMapper.selectSmallGroupVoteDetailForUpdate(smallGroupId, smallGroupVoteId);
        if (vote == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_VOTE_NOT_FOUND", "투표를 찾을 수 없습니다.");
        }
        if (isVoteClosed(vote, LocalDateTime.now(KOREA_ZONE_ID))) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_VOTE_CLOSED", "마감된 투표에는 참여할 수 없습니다.");
        }

        List<Long> selectedOptionIds = normalizeSelectedOptionIds(request, vote);
        List<Long> voteOptionIds = smallGroupMapper.selectSmallGroupVoteOptionIds(smallGroupVoteId);
        if (!new LinkedHashSet<>(voteOptionIds).containsAll(selectedOptionIds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SMALL_GROUP_VOTE_OPTION_INVALID",
                    "선택한 투표 보기가 해당 투표에 속하지 않습니다.");
        }

        Long participationId = smallGroupMapper.selectSmallGroupVoteParticipationIdForUpdate(smallGroupVoteId, memberId);
        if (participationId == null) {
            createSmallGroupVoteParticipation(smallGroupVoteId, memberId, selectedOptionIds);
            return;
        }

        List<Long> previousOptionIds = smallGroupMapper.selectSmallGroupVoteSelectedOptionIds(participationId);
        if (new LinkedHashSet<>(previousOptionIds).equals(new LinkedHashSet<>(selectedOptionIds))) {
            return;
        }
        if (smallGroupMapper.deleteSmallGroupVoteSelections(participationId) != previousOptionIds.size()
                || smallGroupMapper.insertSmallGroupVoteSelections(participationId, selectedOptionIds)
                != selectedOptionIds.size()
                || smallGroupMapper.updateSmallGroupVoteParticipationTime(participationId) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_VOTE_PARTICIPATION_UPDATE_FAILED",
                    "투표 선택 변경 처리에 실패했습니다.");
        }
    }

    // AI작성: 최초 참여는 참여 이력 생성 후 같은 트랜잭션에서 보기 연결을 모두 저장한다.
    private void createSmallGroupVoteParticipation(
            Long smallGroupVoteId, Long memberId, List<Long> selectedOptionIds) {
        if (smallGroupMapper.insertSmallGroupVoteParticipation(smallGroupVoteId, memberId) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_VOTE_PARTICIPATION_CREATE_FAILED",
                    "투표 참여 처리에 실패했습니다.");
        }
        Long participationId =
                smallGroupMapper.selectSmallGroupVoteParticipationIdForUpdate(smallGroupVoteId, memberId);
        if (participationId == null
                || smallGroupMapper.insertSmallGroupVoteSelections(participationId, selectedOptionIds)
                != selectedOptionIds.size()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_VOTE_SELECTION_CREATE_FAILED",
                    "투표 선택 저장에 실패했습니다.");
        }
    }

    // AI작성: 중복 ID·빈 ID·단일 선택 위반을 먼저 막고, 복수 선택은 생성된 보기 수 이하만 허용한다.
    private List<Long> normalizeSelectedOptionIds(
            SmallGroupVoteParticipationRequest request, SmallGroupVoteDetailRow vote) {
        if (request == null || request.getSelectedOptionIds() == null || request.getSelectedOptionIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "투표 보기를 한 개 이상 선택해 주세요.");
        }
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<>();
        for (Long selectedOptionId : request.getSelectedOptionIds()) {
            if (selectedOptionId == null || selectedOptionId <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "선택한 투표 보기 번호가 올바르지 않습니다.");
            }
            if (!normalizedIds.add(selectedOptionId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SMALL_GROUP_VOTE_OPTION_DUPLICATED",
                        "같은 투표 보기를 중복 선택할 수 없습니다.");
            }
        }
        if (!vote.isMultipleChoice() && normalizedIds.size() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SMALL_GROUP_VOTE_SINGLE_CHOICE_REQUIRED",
                    "단일 선택 투표는 한 개의 보기만 선택할 수 있습니다.");
        }
        return List.copyOf(normalizedIds);
    }

    // AI작성: 참여 변경은 기존 생성 권한과 달리 승인된 OWNER·MEMBER 모두 허용하되 관계 행을 잠가 상태 변경과 경합하지 않게 한다.
    private void validateSmallGroupVoteMemberForUpdate(Long groupId, Long memberId) {
        SmallGroupMemberRelationRow relation =
                smallGroupMapper.selectSmallGroupMembershipForUpdate(groupId, memberId);
        boolean isApprovedMember = relation != null
                && "APPROVED".equals(relation.getMembershipStatus())
                && List.of("OWNER", "MEMBER").contains(relation.getMemberRole());
        if (!isApprovedMember) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_PARTICIPATION_FORBIDDEN",
                    "승인된 소모임 회원만 투표에 참여할 수 있습니다.");
        }
    }

    // AI작성: 마감 시각 또는 명시적 종료 상태가 있으면 참여는 막고 결과 전용 상태로 반환한다.
    private boolean isVoteClosed(SmallGroupVoteDetailRow vote, LocalDateTime now) {
        return "CLOSED".equals(vote.getStatus())
                || (vote.getEndsAt() != null && !vote.getEndsAt().isAfter(now));
    }

    // AI작성: 마감·종료 후 24시간만 결과를 유지하고 그 뒤에는 상세 대상에서 제외한다.
    private boolean isVoteClosedBeyondVisiblePeriod(SmallGroupVoteDetailRow vote, LocalDateTime now) {
        if (!isVoteClosed(vote, now)) {
            return false;
        }
        LocalDateTime closedAt = vote.getClosedAt() != null ? vote.getClosedAt() : vote.getEndsAt();
        return closedAt != null && closedAt.plusHours(VOTE_CLOSED_VISIBLE_HOURS).isBefore(now);
    }

    // AI작성: 익명·미참여 요청에는 빈 닉네임 목록만 반환해 보기별 득표 수는 유지하면서 개인정보를 숨긴다.
    private SmallGroupVoteOptionDetailResponse toSmallGroupVoteOptionDetailResponse(
            SmallGroupVoteOptionDetailRow option, Map<Long, List<String>> voterNicknamesByOptionId) {
        return new SmallGroupVoteOptionDetailResponse(
                option.getSmallGroupVoteOptionId(),
                option.getContent(),
                option.getDisplayOrder(),
                option.getVoteCount(),
                voterNicknamesByOptionId.getOrDefault(option.getSmallGroupVoteOptionId(), List.of()));
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

        // AI작성: 투표 생성자 본인을 제외한 승인 소모임 회원에게 새 투표 알림을 보낸다.
        smallGroupMapper.selectApprovedSmallGroupMemberIds(target.getGroupId()).stream()
                .filter(receiverId -> !Objects.equals(receiverId, memberId))
                .forEach(receiverId -> notificationService.send(
                        receiverId,
                        NotificationType.VOTE_CREATED,
                        "새로운 투표가 등록되었습니다.",
                        "/small-groups/" + smallGroupId + "/votes/" + command.getSmallGroupVoteId()
                ));
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
        validateSmallGroupJoinCapacity(target.getMeetDate(), target.getMaxMembers(),
                smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(target.getGroupId()));

        int affectedRows = smallGroupMapper.approveSmallGroupJoinRequest(
                request.getCrewMemberId(), request.getGroupId(), memberId);
        if (affectedRows != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_APPROVAL_FAILED",
                    "가입 신청 상태가 변경되어 승인할 수 없습니다. 다시 확인해주세요.");
        }
        // AI작성: 소모임 가입 승인이 완료된 후 신청 회원에게 처리 결과 알림을 보낸다.
        notificationService.send(
                request.getMemberId(),
                NotificationType.SMALL_GROUP_JOIN_APPROVED,
                "소모임 가입 신청이 승인되었습니다.",
                "/small-groups/" + smallGroupId
        );
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
        // AI작성: 소모임 가입 거절이 완료된 후 신청 회원에게 처리 결과 알림을 보낸다.
        notificationService.send(
                request.getMemberId(),
                NotificationType.SMALL_GROUP_JOIN_REJECTED,
                "소모임 가입 신청이 거절되었습니다.",
                "/small-groups/" + smallGroupId
        );
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

        // AI작성: 본인 관계를 한 번만 읽어 역할 검증과 신청 상태 표시가 같은 결과를 사용한다.
        List<SmallGroupMemberRelationRow> relations = memberId == null ? List.of()
                : smallGroupMapper.selectSmallGroupMemberRelations(row.getGroupId(), memberId);
        String myRole = memberId == null ? null : resolveMemberRole(relations);
        long memberCount = smallGroupMapper.selectApprovedSmallGroupMemberCount(row.getGroupId());
        // AI작성: 비회원도 제목·작성자·작성일을 볼 수 있도록 역할과 무관하게 공지 요약을 함께 조회한다.
        List<SmallGroupNoticeSummaryResponse> notices =
                smallGroupMapper.selectRecentSmallGroupNotices(row.getGroupId());

        // AI작성: 역할과 별도로 본인의 대기·거절·탈퇴·추방 상태를 전달한다.
        SmallGroupMemberRelationRow relation = relations.isEmpty() ? null : relations.get(0);
        String membershipStatus = relation == null ? null : relation.getMembershipStatus();
        JoinAvailability availability = evaluateSmallGroupJoin(row, memberId, memberCount, relation);
        return new SmallGroupDetailResponse(
                row.getSmallGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getLikeCategory(), row.getMeetDate(), row.getLocation(),
                row.getMaxMembers(), memberCount, row.getCreatorNickname(), row.getCreatedAt(), myRole,
                notices, membershipStatus, availability.canApply(), availability.code(), availability.message());
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
        validateSmallGroupJoinMember(member);

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

        validateSmallGroupJoinCapacity(target.getMeetDate(), target.getMaxMembers(),
                smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(target.getGroupId()));

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

        // AI작성: 소모임 가입 신청 저장이 완료된 후 개설자에게 새로운 신청 접수 알림을 보낸다.
        notificationService.send(
                target.getCreatorMemberId(),
                NotificationType.SMALL_GROUP_JOIN_REQUEST,
                "새로운 소모임 가입 신청이 접수되었습니다.",
                "/small-groups/" + smallGroupId
        );
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

    // AI작성: 상세 조회와 실제 신청에서 계정·본인 인증 조건을 동일하게 검사한다.
    private void validateSmallGroupJoinMember(SmallGroupJoinMemberRow member) {
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
    }

    // AI작성: 정원은 승인 인원으로만 판단하고 상세·신청·승인에서 같은 조건을 사용한다.
    private void validateSmallGroupJoinCapacity(LocalDate meetDate, Integer maxMembers, long memberCount) {
        // AI작성: 일정 당일은 허용하며 한국 날짜 기준으로 지난 일정과 미정 일정은 차단한다.
        if (meetDate == null) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_DATE_NOT_CONFIGURED", "소모임 일정이 설정되지 않았습니다.");
        }
        if (meetDate.isBefore(LocalDate.now(KOREA_ZONE_ID))) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_MEETING_ENDED", "일정이 지난 소모임에는 가입 신청할 수 없습니다.");
        }
        if (maxMembers == null || maxMembers <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_NOT_CONFIGURED", "소모임 정원이 설정되지 않았습니다.");
        }
        if (memberCount >= maxMembers) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_FULL", "소모임 정원이 모두 찼습니다.");
        }
    }

    // AI작성: 제한은 정상 상세 응답에 담고 데이터 오류는 숨기지 않고 기존 예외로 전달한다.
    private record JoinAvailability(boolean canApply, String code, String message) {}

    private JoinAvailability evaluateSmallGroupJoin(SmallGroupDetailRow row, Long memberId, long memberCount, SmallGroupMemberRelationRow relation) {
        try {
            if (memberId == null || memberId <= 0) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
            }
            validateExistingMembership(relation);
            validateSmallGroupJoinRole(relation);
            validateSmallGroupJoinMember(smallGroupMapper.selectSmallGroupJoinMember(memberId));
            if (Objects.equals(row.getCreatorMemberId(), memberId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED", "개설자 또는 운영진은 자신의 소모임에 가입 신청할 수 없습니다.");
            }
            validateSmallGroupJoinCapacity(row.getMeetDate(), row.getMaxMembers(), memberCount);
            return new JoinAvailability(true, null, null);
        } catch (ApiException error) {
            if (error.getStatus().is5xxServerError()) throw error;
            return new JoinAvailability(false, error.getCode(), error.getMessage());
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
        return resolveMemberRole(relations);
    }

    // AI작성: 상세 조회에서 이미 읽은 본인 관계를 재사용해 기존 권한 검증을 유지한다.
    private String resolveMemberRole(List<SmallGroupMemberRelationRow> relations) {
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
                // AI작성: 기존 코드 검색과 관심사를 유지하고 새 지역 조건을 함께 전달한다.
                escapeKeyword(request.getKeyword()), regionName,
                request.getProvinceName(), request.getDistrictName(), request.getTownName(),
                request.getLikeCategory(), 0, PAGE_SIZE);
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
