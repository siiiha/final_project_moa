package com.nightstudy.moa.smallGroup.mapper;

import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCategoryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinApprovalTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinMemberRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateCommand;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsAuthorityRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsBasicUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsDetailUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateCommand;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionVoterNicknameRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 소모임 생성과 목록 조회는 기존 MyBatis Mapper/XML에서 함께 관리한다.
@Mapper
public interface SmallGroupMapper {
    // AI작성: 작성자와 모임을 함께 제한해 삭제 시각만 남긴다.
    int deleteSmallGroupNotice(@Param("groupId") Long groupId, @Param("noticeId") Long noticeId, @Param("writerId") Long writerId);

    // AI작성: 모임·관계를 잠근 다음 소속 공지를 잠가 수정·삭제와 고정을 직렬화한다.
    com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeDetailResponse selectSmallGroupNoticeForUpdate(
            @Param("groupId") Long groupId, @Param("noticeId") Long noticeId);
    int updateSmallGroupNotice(@Param("groupId") Long groupId, @Param("noticeId") Long noticeId,
            @Param("writerId") Long writerId, @Param("title") String title, @Param("content") String content);

    // AI작성: 소모임 소속 공지만 조회하며 삭제된 공지는 제외한다.
    com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeDetailResponse selectSmallGroupNoticeDetail(
            @Param("groupId") Long groupId, @Param("noticeId") Long noticeId);

    // AI작성: 정렬은 SQL choose로 제한하고 페이지 크기는 네 건으로 고정한다.
    int countSmallGroupNotices(@Param("groupId") Long groupId);
    List<SmallGroupNoticeSummaryResponse> selectSmallGroupNoticePage(@Param("groupId") Long groupId,
            @Param("offset") int offset, @Param("sort") String sort);

    // AI작성: 소모임 ID를 공통 모임 ID로 검증한 뒤 공지를 저장한다.
    int insertSmallGroupNotice(com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeCreateRow row);

    // AI작성: 공개 상세 조회에서는 행 잠금 없이 본인의 신청 조건만 읽는다.
    SmallGroupJoinMemberRow selectSmallGroupJoinMember(@Param("memberId") Long memberId);

    // AI작성: 같은 소모임 조건으로 후기 전체 건수를 조회해 공통 페이징 정보를 구성한다.
    int selectSmallGroupReviewCount(SmallGroupReviewListCondition condition);

    // AI작성: 검증된 소모임·offset·고정 크기 8을 전달해 최신순 후기 목록을 조회한다.
    List<SmallGroupReviewListItemResponse> selectSmallGroupReviewList(SmallGroupReviewListCondition condition);

    // AI작성: 삭제되지 않은 소모임·개설자를 대상으로 모임 행을 잠가 등록 조건을 조회한다.
    SmallGroupReviewCreateTargetRow selectSmallGroupReviewCreateTargetForUpdate(
            @Param("smallGroupId") Long smallGroupId);

    // AI작성: 회원 관계는 기존 selectSmallGroupMembershipForUpdate를 재사용하고 후기 중복만 별도 확인한다.
    Long selectSmallGroupReviewId(
            @Param("smallGroupId") Long smallGroupId, @Param("writerId") Long writerId);

    // AI작성: 검증된 후기를 한 건 저장하고 생성 키를 command.smallGroupReviewId에 설정한다.
    int insertSmallGroupReview(@Param("command") SmallGroupReviewCreateCommand command);

    List<SmallGroupRegionResponse> selectSmallGroupRegions();

    List<SmallGroupCategoryResponse> selectSmallGroupCategories();

    Long selectRegionIdByName(@Param("regionName") String regionName);

    boolean existsActiveMember(@Param("memberId") Long memberId);

    // AI작성: 종료 상태가 없는 현재 구조에서는 삭제되지 않은 생성 소모임을 생성 한도에 포함한다.
    // TODO: 소모임 종료 상태 정책이 확정되면 deleted_at 대신 확정된 종료 상태를 사용한다.
    long selectActiveCreatedSmallGroupCount(@Param("memberId") Long memberId);

    int insertGroup(SmallGroupCreationRequest request);

    int insertSmallGroup(SmallGroupCreationRequest request);

    int insertOwnerMembership(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    int selectSmallGroupCount(SmallGroupListCondition condition);

    List<SmallGroupRow> selectSmallGroupList(SmallGroupListCondition condition);

    List<SmallGroupMemberCount> selectApprovedMemberCounts(@Param("groupIds") List<Long> groupIds);

    // AI작성: 삭제된 소모임 또는 삭제된 개설자 소모임은 상세 조회 대상에서 제외한다.
    SmallGroupDetailRow selectSmallGroupDetail(@Param("smallGroupId") Long smallGroupId);

    // AI작성: membership_status와 관계없이 모든 관계 행을 반환해 MANAGER·중복 데이터를 숨기지 않는다.
    List<SmallGroupMemberRelationRow> selectSmallGroupMemberRelations(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 소모임 참여 인원은 승인된 OWNER와 MEMBER만 중복 없이 집계한다.
    long selectApprovedSmallGroupMemberCount(@Param("groupId") Long groupId);

    // AI작성: 가입 신청 중 회원 계정·본인 인증·시스템 권한 변경과 경합하지 않도록 회원 행을 잠가 조회한다.
    SmallGroupJoinMemberRow selectSmallGroupJoinMemberForUpdate(@Param("memberId") Long memberId);

    // AI작성: 삭제되지 않은 소모임의 공통 모임 ID·개설자·정원을 잠가 가입 대상 검증에 사용한다.
    SmallGroupJoinTargetRow selectSmallGroupJoinTargetForUpdate(@Param("smallGroupId") Long smallGroupId);

    // AI작성: 같은 회원의 기존 관계를 잠가 역할 제한·중복 신청·LEFT 재신청을 안전하게 판단한다.
    SmallGroupMemberRelationRow selectSmallGroupMembershipForUpdate(
            @Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 가입 신청은 별도 입력 없이 인증 회원을 MEMBER·PENDING 관계로 한 건 저장한다.
    int insertSmallGroupJoinRequest(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 개설자 권한 검증 후 같은 상태 조건으로 신청 수와 페이지 목록을 조회한다.
    int selectSmallGroupJoinRequestCount(@Param("groupId") Long groupId, @Param("status") String status);

    List<SmallGroupJoinRequestRow> selectSmallGroupJoinRequests(
            @Param("groupId") Long groupId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    // AI작성: 신청 회원 잠금 전 식별에만 사용하며 이후 잠금 조회 결과를 승인 근거로 사용한다.
    SmallGroupJoinApprovalTargetRow selectSmallGroupJoinApprovalTarget(
            @Param("crewMemberId") Long crewMemberId);

    // AI작성: 신청 회원·소모임 잠금 뒤 신청 행의 소속과 대기 상태를 다시 확인한다.
    SmallGroupJoinApprovalTargetRow selectSmallGroupJoinApprovalTargetForUpdate(
            @Param("crewMemberId") Long crewMemberId);

    // AI작성: 동일 소모임의 PENDING 신청 한 건만 승인 상태·MEMBER 역할·승인 이력으로 변경한다.
    int approveSmallGroupJoinRequest(@Param("crewMemberId") Long crewMemberId,
            @Param("groupId") Long groupId, @Param("approvedBy") Long approvedBy);

    // AI작성: 신청 ID·소속·PENDING 상태가 모두 일치하는 한 건만 REJECTED로 변경한다.
    int rejectSmallGroupJoinRequest(@Param("crewMemberId") Long crewMemberId,
            @Param("groupId") Long groupId);

    // AI작성: 자진 탈퇴한 LEFT 관계만 기존 승인 정보를 초기화하고 PENDING 신청으로 되돌린다.
    int reactivateLeftSmallGroupMembership(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    // AI작성: 삭제되지 않은 소모임의 개설자와 승인 OWNER를 함께 조회해 설정 접근 권한 검증에 사용한다.
    SmallGroupSettingsAuthorityRow selectSmallGroupSettingsAuthority(
            @Param("smallGroupId") Long smallGroupId);

    // AI작성: 설정 화면에 표시할 현재 수정 가능 정보와 승인 OWNER·MEMBER 수를 조회한다.
    SmallGroupSettingsResponse selectSmallGroupCurrentSettings(
            @Param("smallGroupId") Long smallGroupId);

    // AI작성: 수정 대상 소모임과 공통 모임 행을 잠가 삭제·개설자 권한을 같은 트랜잭션에서 확인한다.
    SmallGroupSettingsAuthorityRow selectSmallGroupSettingsAuthorityForUpdate(
            @Param("smallGroupId") Long smallGroupId);

    // AI작성: 승인 OWNER 관계 행을 잠가 개설자와 OWNER 권한이 수정 직전에 모두 유효한지 확인한다.
    Long selectSmallGroupOwnerMemberIdForUpdate(@Param("groupId") Long groupId);

    // AI작성: 정원 축소 판단 시 승인 OWNER와 MEMBER 관계 행만 잠근 상태로 집계한다.
    long selectApprovedSmallGroupMemberCountForUpdate(@Param("groupId") Long groupId);

    // AI작성: 요청에 포함된 공통 모임 필드만 groups 테이블에 수정한다.
    int updateSmallGroupBasicSettings(@Param("update") SmallGroupSettingsBasicUpdate update);

    // AI작성: 요청에 포함된 소모임 전용 필드만 small_group 테이블에 수정한다.
    int updateSmallGroupDetailSettings(@Param("update") SmallGroupSettingsDetailUpdate update);

    // AI작성: Service가 검증한 투표 기본 정보를 저장하고 생성 키를 command에 설정한다.
    int insertSmallGroupVote(@Param("command") SmallGroupVoteCreateCommand command);

    // AI작성: 같은 투표의 정리된 보기 목록을 입력 순서대로 일괄 저장한다.
    int insertSmallGroupVoteOptions(
            @Param("smallGroupVoteId") Long smallGroupVoteId,
            @Param("options") List<String> options);

    // AI작성: 같은 소모임의 종료되지 않은 투표를 잠금 조회해 동시 생성에서도 진행 중 투표 수를 제한한다.
    long selectOpenSmallGroupVoteCountForUpdate(@Param("smallGroupId") Long smallGroupId);

    // AI작성: 목록 조회 권한 검증 전에 삭제되지 않은 소모임의 공통 모임 ID만 조회한다.
    SmallGroupVoteListTargetRow selectSmallGroupVoteListTarget(@Param("smallGroupId") Long smallGroupId);

    // AI작성: 검색·필터·페이징 없이 해당 소모임의 투표 제목과 등록·마감 일시만 최신순으로 조회한다.
    List<SmallGroupVoteListItemResponse> selectSmallGroupVoteList(@Param("smallGroupId") Long smallGroupId);

    // AI작성: 소모임과 투표 식별자가 함께 일치하고 삭제되지 않은 상세 대상만 조회한다.
    SmallGroupVoteDetailRow selectSmallGroupVoteDetail(
            @Param("smallGroupId") Long smallGroupId,
            @Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 참여 저장 중 투표 행을 잠가 마감·삭제·선택 변경과 동시 요청을 같은 기준으로 처리한다.
    SmallGroupVoteDetailRow selectSmallGroupVoteDetailForUpdate(
            @Param("smallGroupId") Long smallGroupId,
            @Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 삭제 검증을 통과한 소속 투표 한 건만 논리 삭제하고 보기·참여·선택 이력은 보존한다.
    int softDeleteSmallGroupVote(
            @Param("smallGroupId") Long smallGroupId,
            @Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 요청한 보기 ID가 해당 투표에 모두 속하는지 검증할 수 있도록 소속 보기 ID만 반환한다.
    List<Long> selectSmallGroupVoteOptionIds(@Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 투표 상세의 보기를 표시 순서와 현재 득표 수로 함께 조회한다.
    List<SmallGroupVoteOptionDetailRow> selectSmallGroupVoteDetailOptions(
            @Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 탈퇴 여부와 관계없이 저장된 참여 이력 기준의 총 참여 회원 수를 반환한다.
    long selectSmallGroupVoteParticipantCount(@Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 현재 로그인 회원의 중복 없는 참여 이력 식별자를 조회한다.
    Long selectSmallGroupVoteParticipationId(
            @Param("smallGroupVoteId") Long smallGroupVoteId,
            @Param("memberId") Long memberId);

    // AI작성: 기존 참여 행을 잠가 선택 변경과 동시에 같은 회원의 이력이 중복 갱신되지 않게 한다.
    Long selectSmallGroupVoteParticipationIdForUpdate(
            @Param("smallGroupVoteId") Long smallGroupVoteId,
            @Param("memberId") Long memberId);

    // AI작성: 최초 참여 이력을 저장한 뒤 선택 연결을 추가한다.
    int insertSmallGroupVoteParticipation(
            @Param("smallGroupVoteId") Long smallGroupVoteId,
            @Param("memberId") Long memberId);

    // AI작성: 선택 보기 ID 목록을 한 참여 이력에 일괄 연결한다.
    int insertSmallGroupVoteSelections(
            @Param("smallGroupVoteParticipationId") Long smallGroupVoteParticipationId,
            @Param("selectedOptionIds") List<Long> selectedOptionIds);

    // AI작성: 선택 변경 시 기존 보기 연결만 제거하고 참여 이력 행은 유지한다.
    int deleteSmallGroupVoteSelections(@Param("smallGroupVoteParticipationId") Long smallGroupVoteParticipationId);

    // AI작성: 실제 선택이 달라진 재투표만 마지막 참여 시각을 갱신한다.
    int updateSmallGroupVoteParticipationTime(@Param("smallGroupVoteParticipationId") Long smallGroupVoteParticipationId);

    // AI작성: 참여 이력에 연결된 선택 보기 ID만 반환해 내 선택 표시와 참여 API 후 재조회에 사용한다.
    List<Long> selectSmallGroupVoteSelectedOptionIds(
            @Param("smallGroupVoteParticipationId") Long smallGroupVoteParticipationId);

    // AI작성: 공개 투표의 참여 완료 회원에게만 Service가 호출할 보기별 닉네임 조회 계약이다.
    List<SmallGroupVoteOptionVoterNicknameRow> selectSmallGroupVoteOptionVoterNicknames(
            @Param("smallGroupVoteId") Long smallGroupVoteId);

    // AI작성: 공개 상세 화면의 최근 공지 영역에는 최신 4건의 요약만 전달한다.
    List<SmallGroupNoticeSummaryResponse> selectRecentSmallGroupNotices(@Param("groupId") Long groupId);
}
