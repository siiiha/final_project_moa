package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminGroupRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminSmallGroupRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminSmallGroupStatus;
import com.nightstudy.moa.admin.dto.RestrictionAction;
import com.nightstudy.moa.admin.mapper.AdminSmallGroupMapper;
import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 소모임 운영 상태 변경과 관리자 제한 이력을 하나의 트랜잭션으로 처리한다.
public class AdminSmallGroupServiceImpl implements AdminSmallGroupService {
    private final AdminSmallGroupMapper adminSmallGroupMapper;

    public AdminSmallGroupServiceImpl(AdminSmallGroupMapper adminSmallGroupMapper) {
        this.adminSmallGroupMapper = adminSmallGroupMapper;
    }

    @Override
    @Transactional
    public void restrictSmallGroup(Long smallGroupId, AdminGroupRestrictionRequest request) {
        AdminSmallGroupStatus currentStatus = getCurrentStatus(smallGroupId);
        if (currentStatus != AdminSmallGroupStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SMALL_GROUP_RESTRICTION_UNAVAILABLE",
                    "정상 운영 중인 소모임만 운영정지할 수 있습니다."
            );
        }

        changeStatus(
                smallGroupId,
                AdminSmallGroupStatus.ACTIVE,
                AdminSmallGroupStatus.RESTRICTED
        );
        recordRestrictionHistory(smallGroupId, RestrictionAction.RESTRICT, request.getReason());
    }

    @Override
    @Transactional
    public void releaseSmallGroupRestriction(
            Long smallGroupId,
            AdminGroupRestrictionRequest request
    ) {
        AdminSmallGroupStatus currentStatus = getCurrentStatus(smallGroupId);
        String latestAction = adminSmallGroupMapper.selectLatestRestrictionAction(smallGroupId);
        if (currentStatus != AdminSmallGroupStatus.RESTRICTED
                || !RestrictionAction.RESTRICT.name().equals(latestAction)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SMALL_GROUP_NOT_ADMIN_RESTRICTED",
                    "관리자가 운영정지한 소모임만 해제할 수 있습니다."
            );
        }

        changeStatus(
                smallGroupId,
                AdminSmallGroupStatus.RESTRICTED,
                AdminSmallGroupStatus.ACTIVE
        );
        recordRestrictionHistory(smallGroupId, RestrictionAction.RELEASE, request.getReason());
    }

    private AdminSmallGroupStatus getCurrentStatus(Long smallGroupId) {
        AdminSmallGroupRestrictionTarget target =
                adminSmallGroupMapper.selectRestrictionTargetForUpdate(smallGroupId);
        if (target == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "SMALL_GROUP_NOT_FOUND",
                    "소모임을 찾을 수 없습니다."
            );
        }
        return AdminSmallGroupStatus.from(target.getStatus());
    }

    private void changeStatus(
            Long smallGroupId,
            AdminSmallGroupStatus currentStatus,
            AdminSmallGroupStatus nextStatus
    ) {
        if (adminSmallGroupMapper.updateSmallGroupStatus(
                smallGroupId, currentStatus, nextStatus) != 1) {
            throw restrictionFailedException();
        }
    }

    private void recordRestrictionHistory(
            Long smallGroupId,
            RestrictionAction action,
            String reason
    ) {
        if (adminSmallGroupMapper.insertRestrictionHistory(
                smallGroupId, action, reason) != 1) {
            throw restrictionFailedException();
        }
    }

    private ApiException restrictionFailedException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "SMALL_GROUP_RESTRICTION_FAILED",
                "소모임 운영 상태가 변경되어 처리하지 못했습니다."
        );
    }
}
