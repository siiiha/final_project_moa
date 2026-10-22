package com.nightstudy.moa.saving_group.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.saving_group.mapper.SavingGroupMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SavingGroupServiceImpl implements SavingGroupService {

    private final SavingGroupMapper savingGroupMapper;
    private final FileStorageService fileStorageService;

    public SavingGroupServiceImpl(SavingGroupMapper savingGroupMapper, FileStorageService fileStorageService) {
        this.savingGroupMapper = savingGroupMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest) {

        String imagePath = null;

        MultipartFile groupImage = savingGroupCreationRequest.getGroupImage();

        if (groupImage != null && !groupImage.isEmpty()) {
            imagePath = fileStorageService.storeGroupImage(groupImage);
            savingGroupCreationRequest.setGroupImagePath(imagePath);
        }

        // groups 테이블 저장
        int groupsResult = savingGroupMapper.insertGroups(savingGroupCreationRequest);

        // saving-group 테이블 저장
        int savingGroupResult = savingGroupMapper.insertSavingGroup(savingGroupCreationRequest);

        if (groupsResult != 1 || savingGroupResult != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SAVING_GROUP_CREATION_FAILED",
                    "계모임 개설 신청에 실패했습니다."
            );
        }

        return true;

    }

    @Override
    public boolean approveForCreationSavingGroup(Long savingGroupId) {

        // TODO: 계모임에 사용할 가상계좌 번호 발급받는 api 구현 예정

        int result = savingGroupMapper.approveForCreationSavingGroup(savingGroupId);

        if (result != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SAVING_GROUP_APPROVAL_FAILED",
                    "계모임 승인 처리에 실패했습니다."
            );
        }

        return true;
    }

}