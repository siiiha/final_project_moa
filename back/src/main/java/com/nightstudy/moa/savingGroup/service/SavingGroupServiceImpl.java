package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 입력 검증은 Controller에 맡기고 조회와 응답 조합만 담당한다.
@Service
public class SavingGroupServiceImpl implements SavingGroupService {
    private static final int PAGE_SIZE = 6;
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SavingGroupMapper savingGroupMapper;

    // AI작성: 기존 프로젝트의 생성자 주입 방식을 사용한다.
    public SavingGroupServiceImpl(SavingGroupMapper savingGroupMapper) {
        this.savingGroupMapper = savingGroupMapper;
    }

    // AI작성: 모임장을 포함한 승인 회원을 일괄 집계하고 빈 결과는 정상 반환한다.
    @Override
    @Transactional(readOnly = true)
    public SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition request) {
        String regionName = request.getRegionCode() == null ? null
                : REGIONS.get(Integer.parseInt(request.getRegionCode()) - 1);
        SavingGroupListCondition condition = new SavingGroupListCondition(
                escapeKeyword(request.getKeyword()), regionName,
                (request.getPage() - 1L) * PAGE_SIZE, PAGE_SIZE);
        long total = savingGroupMapper.selectSavingGroupCount(condition);
        long totalPages = total / PAGE_SIZE + (total % PAGE_SIZE == 0 ? 0 : 1);
        if (condition.getOffset() >= total) {
            return new SavingGroupListResponse(List.of(), total, totalPages, request.getPage(), PAGE_SIZE);
        }
        List<SavingGroupRow> rows = savingGroupMapper.selectSavingGroupList(condition);
        Map<Long, Long> counts = rows.isEmpty() ? Map.of()
                : savingGroupMapper.selectApprovedMemberCounts(
                        rows.stream().map(SavingGroupRow::getGroupId).distinct().toList())
                .stream().collect(Collectors.toMap(
                        SavingGroupMemberCount::getGroupId, SavingGroupMemberCount::getMemberCount));
        List<SavingGroupSummaryResponse> items = rows.stream().map(row ->
                new SavingGroupSummaryResponse(row.getSavingGroupId(), row.getGroupName(),
                        row.getDescription(), row.getRegionName(), row.getMaxMembers(),
                        counts.getOrDefault(row.getGroupId(), 0L), row.getDuesAmount(),
                        row.getDuesDay(), row.getStatus())).toList();
        return new SavingGroupListResponse(items, total, totalPages, request.getPage(), PAGE_SIZE);
    }

    // AI작성: 바인딩과 별개로 LIKE 특수문자를 일반 문자로 검색하도록 처리한다.
    private String escapeKeyword(String keyword) {
        return keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
