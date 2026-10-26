package com.nightstudy.moa.post.service;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostCreateRequest;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;


    @Autowired
    public PostServiceImpl(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    public List<PostCategoryDto> getPostCategories() {
        return List.of();
    }

    @Override
    public PostListResult getPostList(PostSearchCondition condition) {

        int totalCount = postMapper.selectPostListCount(condition);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), totalCount);

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
        return post;
    }

    @Override
    public Long writePost(Long loginMemberId, PostCreateRequest request) {
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

        return postDto.getPostId();
    }

    @Override
    public Long editPost(Long postId, Long loginMemberId, PostEditRequest request) {
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

        // AI작성: 변경 사항이 없어도 유효한 수정 요청으로 처리한다.
        if (Objects.equals(post.getCategoryId(), request.getCategoryId())
                && Objects.equals(post.getTitle(), request.getTitle().trim())
                && Objects.equals(post.getContent(), request.getContent().trim())) {
            return postId;
        }

        Long editCount = postMapper.editPost(postId, request);

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
}
