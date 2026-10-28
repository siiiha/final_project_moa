package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 목록 조건·페이징·승인 계원 수 조합을 담당한다.
@Service
public class SmallGroupServiceImpl implements SmallGroupService {
    private static final int PAGE_SIZE = 6;
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SmallGroupMapper smallGroupMapper;

    public SmallGroupServiceImpl(SmallGroupMapper smallGroupMapper) {
        this.smallGroupMapper = smallGroupMapper;
    }

    @Override
    public SmallGroupDetailResponse getSmallGroupDetail(Long smallGroupId, Long memberId) {
        // AI작성: Mapper와 공개 조회 정책은 다음 단계에서 구현하며, 계약 추가 시점의 컴파일을 보장한다.
        throw new UnsupportedOperationException("소모임 상세 조회는 아직 구현되지 않았습니다.");
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
