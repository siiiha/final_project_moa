package com.nightstudy.moa.smallGroup.mapper;

import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 소모임 생성은 기존 MyBatis Mapper/XML 관례로 groups, small_group, group_member를 함께 사용한다.
@Mapper
public interface SmallGroupMapper {
    Long selectRegionIdByName(@Param("regionName") String regionName);

    boolean existsActiveMember(@Param("memberId") Long memberId);

    // AI작성: 종료 상태가 없는 현재 구조에서는 삭제되지 않은 생성 소모임을 생성 한도에 포함한다.
    // TODO: 소모임 종료 상태 정책이 확정되면 deleted_at 대신 확정된 종료 상태를 사용한다.
    long selectActiveCreatedSmallGroupCount(@Param("memberId") Long memberId);

    int insertGroup(SmallGroupCreationRequest request);

    int insertSmallGroup(SmallGroupCreationRequest request);

    int insertOwnerMembership(@Param("groupId") Long groupId, @Param("memberId") Long memberId);
}
