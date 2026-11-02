package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.service.ProfileImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// 로그인한 회원 본인의 프로필 사진 요청
@RestController
@RequestMapping("/api/members/me/profile-image")
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    public ProfileImageController(
            ProfileImageService profileImageService
    ) {
        this.profileImageService = profileImageService;
    }

    // 처음 등록할 때와 기존 사진을 변경할 때 함께 사용
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @RequestAttribute("memberId") Long memberId,
            @RequestPart("file") MultipartFile file
    ) {
        String imageUrl =
                profileImageService.uploadProfileImage(memberId, file);

        return ResponseEntity.ok(
                ApiResponse.ok(Map.of("profileImageUrl", imageUrl))
        );
    }

    // 요청한 파일이 본인의 현재 프로필 사진 확인
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getProfileImage(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable("fileName") String fileName
    ) {
        Resource image = profileImageService.getProfileImage(memberId);

        if (!fileName.equals(image.getFilename())) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_IMAGE_NOT_FOUND",
                    "프로필 사진을 찾을 수 없습니다."
            );
        }

        MediaType contentType = fileName.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage(
            @RequestAttribute("memberId") Long memberId
    ) {
        profileImageService.deleteProfileImage(memberId);

        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}