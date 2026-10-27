package com.nightstudy.moa.smallGroup.mapper;

import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 소모임 생성과 목록 조회는 기존 MyBatis Mapper/XML에서 함께 관리한다.
@Mapper
public interface SmallGroupMapper {
    List<SmallGroupRegionResponse> selectSmallGroupRegions();

    Long selectRegionIdByName(@Param("regionName") String regionName);

    boolean existsActiveMember(@Param("memberId") Long memberId);

    // AI작성: 종료 상태가 없는 현재 구조에서는 삭제되지 않은 생성 소모임을 생성 한도에 포함한다.
    // TODO: 소모임 종료 상태 정책이 확정되면 deleted_at 대신 확정된 종료 상태를 사용한다.
    long selectActiveCreatedSmallGroupCount(@Param("memberId") Long memberId);

    int insertGroup(SmallGroupCreationRequest request);

    int insertSmallGroup(SmallGroupCreationRequest request);

    int insertOwnerMembership(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    List<SmallGroupRow> selectSmallGroupList();

    List<SmallGroupMemberCount> selectApprovedMemberCounts(@Param("groupIds") List<Long> groupIds);
}
