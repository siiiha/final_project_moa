package com.nightstudy.moa.common.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Path GROUP_IMAGE_DIRECTORY =
            Paths.get("C:/uploads/groups");

    // 업로드된 이미지 파일을 서버 폴더에 저장하고, DB에 저장할 이미지 경로를 반환하는 함수
    public String storeGroupImage(MultipartFile file) {
        validateImage(file);

        String extension = getExtension(file.getOriginalFilename());
        String savedFileName = UUID.randomUUID() + "." + extension;

        Path targetPath = GROUP_IMAGE_DIRECTORY.resolve(savedFileName);

        try {
            Files.createDirectories(GROUP_IMAGE_DIRECTORY);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }

        // DB에 저장할 상대 경로
        return "/uploads/groups/" + savedFileName;
    }

    // 업로드된 파일의 용량과 파일형식(JPG, PNG, WEBP 등)이 허용 조건에 맞는지 검증
    private void validateImage(MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();

        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {
            throw new IllegalArgumentException("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }
    }

    // 원본 파일명에서 확장자(jpg, png, webp 등)만 추출
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("확장자가 없는 파일입니다.");
        }

        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
