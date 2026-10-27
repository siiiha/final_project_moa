package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.MemberLoginDto;
import com.nightstudy.moa.member.dto.MemberProfileResponse;
import com.nightstudy.moa.member.mapper.MemberMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

// 회원의 프로필 사진 등록·조회·삭제를 처리
@Service
public class ProfileImageServiceImpl implements ProfileImageService {

    private static final String IMAGE_URL_PREFIX =
            "/api/members/me/profile-image/";

    private final MemberMapper memberMapper;
    private final ProfileImageStorage imageStorage;

    public ProfileImageServiceImpl(
            MemberMapper memberMapper,
            ProfileImageStorage imageStorage
    ) {
        this.memberMapper = memberMapper;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public String uploadProfileImage(Long memberId, MultipartFile file) {
        requireEditableMember(memberId);

        MemberProfileResponse profile = requireProfile(memberId);
        String oldFileName = fileNameFromUrl(profile.getProfileImageUrl());

        String newFileName = imageStorage.store(file);
        String newImageUrl = IMAGE_URL_PREFIX + newFileName;

        registerFileCleanup(newFileName, oldFileName);

        int updatedRows =
                memberMapper.updateProfileImageUrl(memberId, newImageUrl);

        if (updatedRows != 1) {
            throw updateFailed();
        }

        return newImageUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getProfileImage(Long memberId) {
        MemberProfileResponse profile = requireProfile(memberId);
        String fileName = fileNameFromUrl(profile.getProfileImageUrl());

        if (fileName == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_IMAGE_NOT_FOUND",
                    "프로필 사진을 찾을 수 없습니다."
            );
        }

        return imageStorage.load(fileName);
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long memberId) {
        requireEditableMember(memberId);

        MemberProfileResponse profile = requireProfile(memberId);

        if (profile.getProfileImageUrl() == null) {
            return;
        }

        String oldFileName = fileNameFromUrl(profile.getProfileImageUrl());

        int updatedRows =
                memberMapper.updateProfileImageUrl(memberId, null);

        if (updatedRows != 1) {
            throw updateFailed();
        }

        registerFileCleanup(null, oldFileName);
    }

    // 같은 회원의 사진 변경 요청이 동시에 처리되지 않도록 잠금
    private void requireEditableMember(Long memberId) {
        MemberLoginDto member =
                memberMapper.findByMemberIdForUpdate(memberId);

        if (member == null) {
            throw memberNotFound();
        }

        if (!"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "현재 상태에서는 회원정보를 변경할 수 없습니다."
            );
        }
    }

    private MemberProfileResponse requireProfile(Long memberId) {
        MemberProfileResponse profile =
                memberMapper.findProfileByMemberId(memberId);

        if (profile == null) {
            throw memberNotFound();
        }

        return profile;
    }

    // 서버가 생성한 사진 경로에서만 파일명을 추출
    private String fileNameFromUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(IMAGE_URL_PREFIX)) {
            return null;
        }

        String fileName = imageUrl.substring(IMAGE_URL_PREFIX.length());

        if (!fileName.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                        + "[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)"
        )) {
            return null;
        }

        return fileName;
    }

    // DB 저장 성공 시 이전 사진을, 실패 시 새 사진을 삭제
    private void registerFileCleanup(
            String newFileName,
            String oldFileName
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            imageStorage.delete(oldFileName);
                        } else {
                            imageStorage.delete(newFileName);
                        }
                    }
                }
        );
    }

    private ApiException memberNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "MEMBER_NOT_FOUND",
                "회원정보를 찾을 수 없습니다."
        );
    }

    private ApiException updateFailed() {
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PROFILE_IMAGE_UPDATE_FAILED",
                "프로필 사진 정보 변경에 실패했습니다."
        );
    }
}
