package com.nightstudy.moa.post.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostCreateRequest;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.service.PostService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;



@Validated
@RestController
@RequestMapping("/api/community")
public class PostController {
    private static final String VISITOR_COOKIE_NAME = "moa_visitor_id";
    private static final Duration VISITOR_COOKIE_MAX_AGE = Duration.ofDays(365);
    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(value = "/write", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Long> write(
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long postId = postService.writePost(loginMemberId, request);
        return ApiResponse.ok(postId);
    }

    // AI작성: 게시글과 첨부 이미지를 함께 받습니다.
    @PostMapping(value = "/write", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> writeWithImages(
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestPart("post") PostCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ApiResponse.ok(postService.writePost(loginMemberId, request, images == null ? List.of() : images));
    }

    @GetMapping("/list")
    public ApiResponse<List<PostDto>> list(
            @ModelAttribute PostSearchCondition condition
    ) {
        PostListResult result = postService.getPostList(condition);

        return ApiResponse.ok(
                result.getPostList(),
                result.getPageMeta()
        );
    }

    @GetMapping("/categories")
    public ApiResponse<List<PostCategoryDto>> categories() {
        return ApiResponse.ok(postService.getPostCategories());
    }

    // AI작성: 공개 상세는 비로그인 요청에도 회원 번호 없이 조회할 수 있습니다.
    @GetMapping("/detail/{postId}")
    public ApiResponse<PostDto> detail(
            @PathVariable
            @Positive(message = "게시글 번호는 1 이상이어야 합니다.")
            Long postId,
            @RequestAttribute(value = "memberId", required = false) Long loginMemberId,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String visitorId = loginMemberId == null
                ? resolveVisitorId(servletRequest, servletResponse)
                : null;
        PostDto post = postService.getPostDetail(postId, loginMemberId, visitorId);
        return ApiResponse.ok(post);
    }

    private String resolveVisitorId(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String visitorId = request.getCookies() == null ? null
                : Arrays.stream(request.getCookies())
                .filter(cookie -> VISITOR_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(this::isValidVisitorId)
                .findFirst()
                .orElse(null);

        if (visitorId != null) {
            return visitorId;
        }

        visitorId = UUID.randomUUID().toString();
        // AI작성: 개인정보 대신 임의 UUID만 저장하며 운영 HTTPS에서는 Secure 속성도 적용합니다.
        ResponseCookie visitorCookie = ResponseCookie.from(VISITOR_COOKIE_NAME, visitorId)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(VISITOR_COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, visitorCookie.toString());
        return visitorId;
    }

    private boolean isValidVisitorId(String value) {
        if (value == null) {
            return false;
        }
        try {
            return value.equals(UUID.fromString(value).toString());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    // AI작성: 첨부 파일의 접근 권한을 요청마다 검사할 수 있도록 캐시를 사용하지 않습니다.
    @GetMapping("/{postId}/images/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable @Positive Long postId, @PathVariable String fileName) {
        Resource image = postService.getPostImage(postId, fileName);
        return ResponseEntity.ok()
                .contentType(fileName.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image);
    }

    @PutMapping(value = "/edit/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Long> editPost(
            @PathVariable
            @Positive(message = "게시글 번호는 1 이상이어야 합니다.")
            Long postId,
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody PostEditRequest request) {
        Long editPostId = postService.editPost(postId, loginMemberId, request);

        return ApiResponse.ok(editPostId);
    }

    // AI작성: 기존 첨부의 유지 목록과 새 파일을 함께 받습니다.
    @PutMapping(value = "/edit/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> editWithImages(
            @PathVariable @Positive Long postId,
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestPart("post") PostEditRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ApiResponse.ok(postService.editPost(postId, loginMemberId, request,
                images == null ? List.of() : images));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable
            @Positive(message = "게시글 번호는 1 이상이어야 합니다.")
            Long postId,
            @RequestAttribute("memberId") Long loginMemberId) {
        postService.deletePost(postId, loginMemberId);

        return ApiResponse.ok(null);
    }
}
