package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 목록 조회와 승인 계원 수 조합만 담당하며 검색·필터·페이징은 후속 단계에서 추가한다.
@Service
public class SmallGroupServiceImpl implements SmallGroupService {
    private final SmallGroupMapper smallGroupMapper;

    public SmallGroupServiceImpl(SmallGroupMapper smallGroupMapper) {
        this.smallGroupMapper = smallGroupMapper;
    }

    // AI작성: 생성 즉시 공개되는 소모임 정책에 따라 삭제되지 않은 소모임만 Mapper에서 조회한다.
    @Override
    @Transactional(readOnly = true)
    public List<SmallGroupSummaryResponse> getSmallGroupList() {
        List<SmallGroupRow> rows = smallGroupMapper.selectSmallGroupList();
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> memberCounts = smallGroupMapper.selectApprovedMemberCounts(
                        rows.stream().map(SmallGroupRow::getGroupId).distinct().toList())
                .stream().collect(Collectors.toMap(
                        SmallGroupMemberCount::getGroupId, SmallGroupMemberCount::getMemberCount));

        return rows.stream().map(row -> new SmallGroupSummaryResponse(
                row.getSmallGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getMaxMembers(), memberCounts.getOrDefault(row.getGroupId(), 0L),
                row.getLikeCategory(), row.getMeetDate(), row.getLocation())).toList();
    }
}
