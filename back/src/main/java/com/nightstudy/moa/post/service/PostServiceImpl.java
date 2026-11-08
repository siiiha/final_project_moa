package com.nightstudy.moa.post.service;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.member.service.restriction.MemberFeatureRestrictionService;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostCreateRequest;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService {
    private static final int MAX_IMAGES = 3;
    private final PostImageStorage imageStorage;
    private final PostMapper postMapper;
    private final MemberFeatureRestrictionService restrictionService;


    @Autowired
    public PostServiceImpl(
            PostMapper postMapper,
            MemberFeatureRestrictionService restrictionService,
            PostImageStorage imageStorage
    ) {
        this.postMapper = postMapper;
        this.imageStorage = imageStorage;
        this.restrictionService = restrictionService;
    }

    @Override
    public List<PostCategoryDto> getPostCategories() {
        return postMapper.selectPostCategories();
    }

    @Override
    public PostListResult getPostList(PostSearchCondition condition) {

        int totalCount = postMapper.selectPostListCount(condition);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), totalCount);

        // AI작성: 조회 결과가 없으면 동일한 검색 조건의 목록 쿼리를 다시 실행하지 않고 빈 목록을 반환합니다.
        if (totalCount == 0) {
            return new PostListResult(List.of(), pageMeta);
        }

        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        List<PostDto> postList = postMapper.selectPostList(condition);
        return new PostListResult(postList, pageMeta);
    }

    @Override
//    게시글 번호를 받아서 → 조회수를 증가시키고 → 게시글 정보를 조회하고 → 이미지까지 추가한 다음 → 완성된 게시글 정보를 반환하는 코드
    public PostDto getPostDetail(
            Long postId,
            Long loginMemberId
    ) {
        int updatedRows =
                postMapper.increaseViewCount(postId, loginMemberId);

        PostDto post = postMapper.selectPostDetail(postId);

        if (post == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }

        if (updatedRows == 1) {
            System.out.println("조회수 증가 성공");
        } else {
            System.out.println("작성자 본인이거나 존재하지 않는 게시글입니다.");
        }
        post.setImages(postMapper.selectPostImages(postId));
        return post;
    }

    // AI작성: 상세 조회와 같은 게시글 조건을 적용하고 파일의 소속을 확인합니다.
    @Override
    public Resource getPostImage(Long postId, String fileName) {
        String imageUrl = postImagePrefix(postId) + fileName;
        if (postMapper.selectPostDetail(postId) == null
                || postMapper.selectPostImages(postId).stream().noneMatch(image -> imageUrl.equals(image.getImageUrl()))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "POST_IMAGE_NOT_FOUND", "게시글 사진을 찾을 수 없습니다.");
        }
        return imageStorage.load(fileName);
    }

    private String postImagePrefix(Long postId) {
        return "/api/community/" + postId + "/images/";
    }

    @Override
    public Long writePost(Long loginMemberId, PostCreateRequest request) {
        return writePost(loginMemberId, request, List.of());
    }

    // AI작성: 게시글과 첨부 DB 작업을 같은 트랜잭션에서 처리합니다.
    @Override
    @Transactional
    public Long writePost(Long loginMemberId, PostCreateRequest request, List<MultipartFile> images) {
        validateImageCount(images.size());
        validateContentAllowed(loginMemberId);

        int categoryCount =
                postMapper.countCategoryById(request.getCategoryId());

        if (categoryCount != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "존재하지 않는 카테고리입니다."
            );
        }

        // AI작성: 작성자 번호는 클라이언트 입력이 아닌 JWT에서 검증된 회원 번호만 사용한다.
        PostDto postDto = new PostDto();
        postDto.setWriterId(loginMemberId);
        postDto.setCategoryId(request.getCategoryId());
        postDto.setTitle(request.getTitle().trim());
        postDto.setContent(request.getContent().trim());
        int insertedRows = postMapper.insertPost(postDto);

        if (insertedRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "POST_WRITE_FAILED",
                    "게시글 등록에 실패했습니다."
            );
        }

        if (!images.isEmpty()) {
            saveImages(postDto.getPostId(), images, 0);
        }
        return postDto.getPostId();
    }

    private void validateImageCount(int count) {
        if (count > MAX_IMAGES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "POST_IMAGE_LIMIT_EXCEEDED",
                    "게시글에는 사진을 최대 3장 첨부할 수 있습니다.");
        }
    }

    private void saveImages(Long postId, List<MultipartFile> images, int sortOrder) {
        List<String> storedFiles = new ArrayList<>();
        // AI작성: 후속 파일 저장이나 DB 작업이 실패하면 이번 요청의 파일을 정리합니다.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storedFiles.forEach(imageStorage::delete);
                }
            }
        });

        for (MultipartFile image : images) {
            String fileName = imageStorage.store(image);
            storedFiles.add(fileName);
            String imageUrl = postImagePrefix(postId) + fileName;
            if (postMapper.insertPostImage(postId, imageUrl, sortOrder++) != 1) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "POST_IMAGE_SAVE_FAILED",
                        "게시글 사진 저장에 실패했습니다.");
            }
        }
    }

    @Override
    public Long editPost(Long postId, Long loginMemberId, PostEditRequest request) {
        validateContentAllowed(loginMemberId);

        PostDto post = postMapper.selectPostDetail(postId);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }
        // AI작성: 수정도 삭제와 동일하게 작성자 본인만 가능하다.
        if (!Objects.equals(post.getWriterId(), loginMemberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "POST_EDIT_FORBIDDEN", "게시글 수정 권한이 없습니다.");
        }

        int categoryCount =
                postMapper.countCategoryById(request.getCategoryId());

        if (categoryCount != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "존재하지 않는 카테고리입니다."
            );
        }

        // AI작성: 게시글 작성과 동일하게 수정 요청의 앞뒤 공백을 제거해 비교값과 저장값을 일치시킨다.
        request.setTitle(request.getTitle().trim());
        request.setContent(request.getContent().trim());

        // AI작성: 변경 사항이 없어도 유효한 수정 요청으로 처리한다.
        if (Objects.equals(post.getCategoryId(), request.getCategoryId())
                && Objects.equals(post.getTitle(), request.getTitle())
                && Objects.equals(post.getContent(), request.getContent())) {
            return postId;
        }

        int editCount =
                postMapper.editPost(postId, loginMemberId, request);

        if (editCount != 1) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }
        return postId;
    }


    @Override
    public void deletePost(Long postId, Long loginMemberId) {
        validateContentAllowed(loginMemberId);

        PostDto post = postMapper.selectPostDetail(postId);

        if (post == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }

        if (!Objects.equals(post.getWriterId(), loginMemberId)){
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "POST_DELETE_FORBIDDEN",
                    "게시글 삭제 권한이 없습니다."
            );
        }

        int deletedRows = postMapper.deletePost(postId, loginMemberId);

        if (deletedRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "POST_DELETE_FAILED",
                    "게시글 삭제에 실패했습니다."
            );
        }
    }

    // AI작성: 게시글 작성·수정·삭제에서 사용하는 CONTENT 제한 검사를 한 곳에서 처리합니다.
    private void validateContentAllowed(Long memberId) {
        restrictionService.validateAllowed(
                memberId,
                MemberRestrictionScope.CONTENT
        );
    }
}
