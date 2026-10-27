package com.nightstudy.moa.member.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageService {

    String uploadProfileImage(Long memberId, MultipartFile file);
    Resource getProfileImage(Long memberId);
    void deleteProfileImage(Long memberId);

}
