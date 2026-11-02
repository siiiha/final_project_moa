package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import com.nightstudy.moa.admin.dto.AdminSavingGroupRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminSavingGroupRestrictionTarget;
import com.nightstudy.moa.admin.mapper.AdminSavingGroupMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMemberCount;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 관리자 계모임 조회의 페이징과 대상 없음 예외를 처리한다.
public class AdminSavingGroupServiceImpl implements AdminSavingGroupService {
    private final AdminSavingGroupMapper adminSavingGroupMapper;
    private final SavingGroupMapper savingGroupMapper;
    private final SavingGroupService savingGroupService;

    public AdminSavingGroupServiceImpl(
            AdminSavingGroupMapper adminSavingGroupMapper,
            SavingGroupMapper savingGroupMapper,
            SavingGroupService savingGroupService
    ) {
        this.adminSavingGroupMapper = adminSavingGroupMapper;
        this.savingGroupMapper = savingGroupMapper;
        this.savingGroupService = savingGroupService;
    }

    @Override
    public AdminSavingGroupListResult getSavingGroupList(AdminSavingGroupSearchCondition condition) {
        int total = adminSavingGroupMapper.selectSavingGroupCount(condition);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), total);

        if (total == 0) {
            return new AdminSavingGroupListResult(List.of(), pageMeta);
        }

        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());
        List<AdminSavingGroupListItemResponse> savingGroups = adminSavingGroupMapper.selectSavingGroupList(condition);
        // AI작성: 관리자 목록도 기존 계모임 Mapper의 일괄 회원 수 집계를 재사용한다.
        Map<Long, Long> memberCounts = savingGroups.isEmpty() ? Map.of()
                : savingGroupMapper.selectApprovedMemberCounts(
                                savingGroups.stream().map(AdminSavingGroupListItemResponse::getGroupId).distinct().toList())
                        .stream().collect(Collectors.toMap(
                                SavingGroupMemberCount::getGroupId,
                                SavingGroupMemberCount::getMemberCount));
        savingGroups.forEach(item -> item.setMemberCount(Math.toIntExact(
                memberCounts.getOrDefault(item.getGroupId(), 0L))));
        return new AdminSavingGroupListResult(savingGroups, pageMeta);
    }

    @Override
    public AdminSavingGroupDetailResponse getSavingGroupDetail(Long savingGroupId) {
        AdminSavingGroupDetailResponse detail =
                adminSavingGroupMapper.selectSavingGroupDetail(savingGroupId);
        if (detail == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "SAVING_GROUP_NOT_FOUND",
                    "계모임을 찾을 수 없습니다."
            );
        }
        return detail;
    }

    @Override
    @Transactional
    // AI작성: 진행 중 상태를 잠근 뒤 기존 상태 전환 로직과 공통 이력 테이블을 같은 트랜잭션에서 사용한다.
    public void restrictSavingGroup(
            Long savingGroupId,
            AdminSavingGroupRestrictionRequest request
    ) {
        AdminSavingGroupRestrictionTarget target = getRestrictionTarget(savingGroupId);
        if (!"02".equals(target.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SAVING_GROUP_RESTRICTION_UNAVAILABLE",
                    "진행 중인 계모임만 이용 제한할 수 있습니다."
            );
        }

        // AI작성: 기존 전환 기능을 재사용해 상태 변경과 보류 중인 회비 설정 정지를 함께 처리한다.
        savingGroupService.changeSavingGroupStatus(target.getGroupId(), "03");
        insertRestrictionHistory(savingGroupId, "RESTRICT", request.getReason());
    }

    @Override
    @Transactional
    // AI작성: 최근 이력이 RESTRICT인 관리자 제한 계모임만 해제해 일반 일시정지를 잘못 복구하지 않는다.
    public void releaseSavingGroupRestriction(
            Long savingGroupId,
            AdminSavingGroupRestrictionRequest request
    ) {
        AdminSavingGroupRestrictionTarget target = getRestrictionTarget(savingGroupId);
        if (!"03".equals(target.getStatus())
                || !"RESTRICT".equals(adminSavingGroupMapper.selectLatestRestrictionAction(savingGroupId))) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SAVING_GROUP_NOT_ADMIN_RESTRICTED",
                    "관리자가 이용 제한한 계모임만 제한을 해제할 수 있습니다."
            );
        }

        // AI작성: 기존 전환 기능을 재사용해 보류했던 회비 설정 일정도 정상적으로 재계산한다.
        savingGroupService.changeSavingGroupStatus(target.getGroupId(), "02");
        insertRestrictionHistory(savingGroupId, "RELEASE", request.getReason());
    }

    private AdminSavingGroupRestrictionTarget getRestrictionTarget(Long savingGroupId) {
        AdminSavingGroupRestrictionTarget target =
                adminSavingGroupMapper.selectRestrictionTargetForUpdate(savingGroupId);
        if (target == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "SAVING_GROUP_NOT_FOUND",
                    "계모임을 찾을 수 없습니다."
            );
        }
        return target;
    }

    private void insertRestrictionHistory(Long savingGroupId, String action, String reason) {
        if (adminSavingGroupMapper.insertRestrictionHistory(savingGroupId, action, reason) != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SAVING_GROUP_RESTRICTION_FAILED",
                    "계모임 이용 제한 상태가 변경되어 처리하지 못했습니다."
            );
        }
    }

}
