package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 기존 saving_group 구현을 savingGroup 도메인과 명확한 빈 이름으로 통합한다.
@Service
public class SavingGroupCreationServiceImpl implements SavingGroupCreationService {

    private final SavingGroupMapper savingGroupMapper;
    private final FileStorageService fileStorageService;

    public SavingGroupCreationServiceImpl(SavingGroupMapper savingGroupMapper, FileStorageService fileStorageService) {
        this.savingGroupMapper = savingGroupMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest) {
        MultipartFile groupImage = savingGroupCreationRequest.getGroupImage();
        if (groupImage != null && !groupImage.isEmpty()) {
            String imagePath = fileStorageService.storeGroupImage(groupImage);
            savingGroupCreationRequest.setGroupImagePath(imagePath);
        }

        int groupsResult = savingGroupMapper.insertGroups(savingGroupCreationRequest);
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
