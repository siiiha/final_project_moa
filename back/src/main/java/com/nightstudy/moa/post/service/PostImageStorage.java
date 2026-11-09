package com.nightstudy.moa.post.service;

import com.nightstudy.moa.common.service.ImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

// AI작성: 게시글 저장 경로와 오류 코드를 공통 이미지 저장 객체에 전달합니다.
@Component
public class PostImageStorage extends ImageStorage {

    public PostImageStorage(
            @Value("${moa.upload.post-dir}") String directory,
            @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize
    ) {
        super(directory, maxFileSize, "POST");
    }
}
