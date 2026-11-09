package com.nightstudy.moa.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

// AI작성: 프로필 저장 경로와 오류 코드를 공통 이미지 저장 객체에 전달합니다.
@Component
public class ProfileImageStorage extends ImageStorage {

    public ProfileImageStorage(
            @Value("${moa.upload.profile-dir}") String directory,
            @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize
    ) {
        super(directory, maxFileSize, "PROFILE");
    }
}
