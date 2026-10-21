package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCurrentSettingsResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMemberCount;
import com.nightstudy.moa.savingGroup.dto.SavingGroupRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsAuthorityRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsBasicUpdate;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSummaryResponse;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
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
                0, PAGE_SIZE);
        int total = savingGroupMapper.selectSavingGroupCount(condition);
        PageMeta pageMeta = PageMeta.of(request.getPage(), PAGE_SIZE, total);
        // AI작성: 범위 초과는 offset 계산 전에 반환하여 큰 페이지 번호의 int 곱셈을 피한다.
        if (pageMeta.getPage() > pageMeta.getTotalPages()) {
            return new SavingGroupListResponse(List.of(), pageMeta);
        }
        condition.setOffset(pageMeta.getOffset());
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
        return new SavingGroupListResponse(items, pageMeta);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGroupSettingsResponse getSavingGroupSettings(Long savingGroupId, Long loginMemberId) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupSettingsAuthorityRow authority =
                savingGroupMapper.selectSavingGroupSettingsAuthority(savingGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        boolean isOwner = Objects.equals(authority.getGroupLeaderMemberId(), loginMemberId)
                && Objects.equals(authority.getOwnerMemberId(), loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_SETTINGS_FORBIDDEN",
                    "계주만 설정을 조회할 수 있습니다.");
        }

        if ("04".equals(authority.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "종료된 계모임의 설정은 조회할 수 없습니다.");
        }

        List<String> editableFields = getEditableFields(authority.getStatus());
        if (editableFields == null) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "현재 상태에서는 계모임 설정을 조회할 수 없습니다.");
        }

        SavingGroupCurrentSettingsResponse current =
                savingGroupMapper.selectSavingGroupCurrentSettings(savingGroupId);
        if (current == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        // TODO: 예정 설정 테이블 추가 후 적용 예정 설정을 별도 Mapper 함수로 조회한다.

        return new SavingGroupSettingsResponse(
                authority.getSavingGroupId(),
                authority.getStatus(),
                true,
                !editableFields.isEmpty(),
                current,
                null,
                editableFields
        );
    }

    @Override
    @Transactional
    public void updateSavingGroupSettings(
            Long savingGroupId, Long loginMemberId, SavingGroupSettingsPatchRequest request) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SavingGroupSettingsAuthorityRow authority =
                savingGroupMapper.selectSavingGroupSettingsAuthorityForUpdate(savingGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임 정보를 찾을 수 없습니다.");
        }

        Long ownerMemberId = savingGroupMapper.selectSavingGroupOwnerMemberIdForUpdate(authority.getGroupId());
        boolean isOwner = Objects.equals(authority.getGroupLeaderMemberId(), loginMemberId)
                && Objects.equals(ownerMemberId, loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_SETTINGS_FORBIDDEN",
                    "계주만 설정을 변경할 수 있습니다.");
        }

        validateEditableFields(authority.getStatus(), request);
        normalizeBasicSettings(request);

        SavingGroupSettingsBasicUpdate update = createBasicSettingsUpdate(authority.getGroupId(), request);

        if (update.getMaxMembers() != null) {
            long approvedMemberCount =
                    savingGroupMapper.selectApprovedMemberCountForUpdate(authority.getGroupId());
            if (update.getMaxMembers() < approvedMemberCount) {
                throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_BELOW_MEMBERS",
                        "승인된 계원 수보다 작게 설정할 수 없습니다.");
            }
        }

        savingGroupMapper.updateSavingGroupBasicSettings(update);
    }

    private void validateEditableFields(String status, SavingGroupSettingsPatchRequest request) {
        if ("03".equals(status) || "04".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "현재 상태에서는 계모임 정보를 수정할 수 없습니다.");
        }

        if (!"00".equals(status) && !"01".equals(status) && !"02".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_NOT_EDITABLE",
                    "현재 상태에서는 계모임 정보를 수정할 수 없습니다.");
        }

        if ("02".equals(status) && request.isMaxMembersProvided()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_FIELD_NOT_EDITABLE",
                    "진행 중인 계모임에서는 정원을 수정할 수 없습니다.");
        }

        if (request.isDuesAmountProvided() || request.isDuesDayProvided()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_FIELD_NOT_EDITABLE",
                    "회비와 납부일 수정은 다음 달 적용 기능에서 처리됩니다.");
        }
    }

    private void normalizeBasicSettings(SavingGroupSettingsPatchRequest request) {
        if (request.isGroupNameProvided()) {
            request.setGroupName(request.getGroupName().strip());
        }
        if (request.isDescriptionProvided()) {
            request.setDescription(request.getDescription().strip());
        }
    }

    private SavingGroupSettingsBasicUpdate createBasicSettingsUpdate(
            Long groupId, SavingGroupSettingsPatchRequest request) {
        String groupName = request.isGroupNameProvided() ? request.getGroupName() : null;
        String description = request.isDescriptionProvided() ? request.getDescription() : null;
        Integer maxMembers = request.isMaxMembersProvided() ? request.getMaxMembers() : null;

        return new SavingGroupSettingsBasicUpdate(
                groupId, groupName, description, maxMembers);
    }

    private List<String> getEditableFields(String status) {
        return switch (status) {
            case "00", "01" -> List.of("groupName", "description", "maxMembers", "duesAmount", "duesDay");
            case "02" -> List.of("groupName", "description", "duesAmount", "duesDay");
            case "03" -> List.of();
            default -> null;
        };
    }

    // AI작성: 바인딩과 별개로 LIKE 특수문자를 일반 문자로 검색하도록 처리한다.
    private String escapeKeyword(String keyword) {
        return keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
