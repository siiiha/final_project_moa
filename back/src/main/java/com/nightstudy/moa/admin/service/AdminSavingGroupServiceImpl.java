package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import com.nightstudy.moa.admin.mapper.AdminSavingGroupMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// AI작성: 관리자 계모임 조회의 페이징과 대상 없음 예외를 처리한다.
public class AdminSavingGroupServiceImpl implements AdminSavingGroupService {
    private final AdminSavingGroupMapper adminSavingGroupMapper;

    public AdminSavingGroupServiceImpl(AdminSavingGroupMapper adminSavingGroupMapper) {
        this.adminSavingGroupMapper = adminSavingGroupMapper;
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
        return new AdminSavingGroupListResult(
                adminSavingGroupMapper.selectSavingGroupList(condition),
                pageMeta
        );
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
}
