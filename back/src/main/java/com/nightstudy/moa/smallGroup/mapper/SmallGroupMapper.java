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
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsAuthorityRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsBasicUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsDetailUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateCommand;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 소모임 생성과 목록 조회는 기존 MyBatis Mapper/XML에서 함께 관리한다.
@Mapper
public interface SmallGroupMapper {
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

    // AI작성: 공개 상세 화면의 최근 공지 영역에는 최신 4건의 요약만 전달한다.
    List<SmallGroupNoticeSummaryResponse> selectRecentSmallGroupNotices(@Param("groupId") Long groupId);
}
