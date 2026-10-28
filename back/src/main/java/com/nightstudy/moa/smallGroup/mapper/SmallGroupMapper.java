package com.nightstudy.moa.smallGroup.mapper;

import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCategoryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsAuthorityRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
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

    // AI작성: 삭제되지 않은 소모임의 개설자와 승인 OWNER를 함께 조회해 설정 접근 권한 검증에 사용한다.
    SmallGroupSettingsAuthorityRow selectSmallGroupSettingsAuthority(
            @Param("smallGroupId") Long smallGroupId);

    // AI작성: 설정 화면에 표시할 현재 수정 가능 정보와 승인 OWNER·MEMBER 수를 조회한다.
    SmallGroupSettingsResponse selectSmallGroupCurrentSettings(
            @Param("smallGroupId") Long smallGroupId);

    // AI작성: 공개 상세 화면의 최근 공지 영역에는 최신 4건의 요약만 전달한다.
    List<SmallGroupNoticeSummaryResponse> selectRecentSmallGroupNotices(@Param("groupId") Long groupId);
}
