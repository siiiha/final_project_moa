package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteParticipationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewSearchCondition;
import java.util.List;

// AI작성: 소모임 공개 목록의 검색·필터 조회 계약이다.
public interface SmallGroupService {
    // AI작성: 소모임장이며 작성자 본인인 공지만 논리 삭제한다.
    void deleteSmallGroupNotice(Long smallGroupId, Long noticeId, Long memberId);

    // AI작성: 현재 소모임장이면서 작성자 본인인 공지만 수정한다.
    Long updateSmallGroupNotice(Long smallGroupId, Long noticeId, Long memberId, String title, String content);

    // AI작성: 승인 OWNER·MEMBER에게만 소속 공지 본문을 제공한다.
    com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeDetailResponse getSmallGroupNoticeDetail(Long smallGroupId, Long noticeId, Long memberId);

    // AI작성: 공개 상세에서 검증한 공통 모임 ID로 목록·페이지 메타를 조회한다.
    com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeListResponse getSmallGroupNotices(Long groupId,
            com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSearchCondition condition);

    // AI작성: 승인된 해당 소모임장만 공지를 작성한다.
    void createSmallGroupNotice(Long smallGroupId, Long memberId, String title, String content);

    // AI작성: 서버 인증 회원 본인의 후기 작성 여부만 조회하고 작성자 정보는 반환하지 않는다.
    boolean hasWrittenSmallGroupReview(Long smallGroupId, Long memberId);

    // AI작성: 비로그인·미가입 사용자도 후기를 조회하며 해당 소모임 MANAGER는 접근을 제한한다.
    SmallGroupReviewListResponse getSmallGroupReviews(
            Long smallGroupId, Long memberId, SmallGroupReviewSearchCondition condition);

    // AI작성: 승인된 일반 모임원이 모임 다음 날부터 한 번만 후기를 등록하고 생성 ID를 반환한다.
    Long createSmallGroupReview(Long smallGroupId, Long memberId, SmallGroupReviewCreateRequest request);

    SmallGroupListResponse getSmallGroupList(SmallGroupSearchCondition condition);

    // AI작성: memberId가 null인 비로그인 공개 조회도 허용하는 상세 조회 계약이다.
    SmallGroupDetailResponse getSmallGroupDetail(Long smallGroupId, Long memberId);

    // AI작성: 인증된 일반 회원 본인 명의로 소모임 가입 신청을 대기 상태로 등록한다.
    void requestSmallGroupJoin(Long smallGroupId, Long memberId);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 상태별 가입 신청 목록을 조회한다.
    SmallGroupJoinRequestListResponse getSmallGroupJoinRequests(
            Long smallGroupId, Long memberId, String status, int page);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 대기 중인 소모임 가입 신청을 승인한다.
    void approveSmallGroupJoin(Long smallGroupId, Long crewMemberId, Long memberId);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 대기 중인 소모임 가입 신청을 거절한다.
    void rejectSmallGroupJoin(Long smallGroupId, Long crewMemberId, Long memberId);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 소모임 설정의 현재 수정 가능 정보를 조회한다.
    SmallGroupSettingsResponse getSmallGroupSettings(Long smallGroupId, Long loginMemberId);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 전달한 소모임 설정 필드를 수정한다.
    void updateSmallGroupSettings(
            Long smallGroupId, Long loginMemberId, SmallGroupSettingsPatchRequest request);

    // AI작성: 삭제되지 않은 소모임의 개설자이면서 승인 OWNER인 회원만 투표와 보기를 함께 생성한다.
    SmallGroupVoteCreateResponse createSmallGroupVote(
            Long smallGroupId, Long memberId, SmallGroupVoteCreateRequest request);

    // AI작성: 승인된 소모임 OWNER 또는 MEMBER만 해당 소모임의 투표 목록을 조회한다.
    List<SmallGroupVoteListItemResponse> getSmallGroupVotes(Long smallGroupId, Long memberId);

    // AI작성: 승인된 소모임 OWNER 또는 MEMBER만 소속 투표의 상세·결과를 정책 범위 안에서 조회한다.
    SmallGroupVoteDetailResponse getSmallGroupVoteDetail(
            Long smallGroupId, Long smallGroupVoteId, Long memberId);

    // AI작성: 투표 작성자이면서 개설자·승인 OWNER인 회원만 OPEN·CLOSED 투표를 논리 삭제한다.
    void deleteSmallGroupVote(Long smallGroupId, Long smallGroupVoteId, Long memberId);

    // AI작성: 승인 회원의 최초 참여는 저장하고, 기존 참여자는 선택 변경이 있을 때만 이력을 갱신한다.
    void participateSmallGroupVote(
            Long smallGroupId, Long smallGroupVoteId, Long memberId,
            SmallGroupVoteParticipationRequest request);

    // AI작성: 참여 이력이 있는 승인 회원만 항목별 득표·참여자 정보를 조회한다.
    SmallGroupVoteDetailResponse getSmallGroupVoteStatus(
            Long smallGroupId, Long smallGroupVoteId, Long memberId);
}
