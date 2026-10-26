package com.nightstudy.moa.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 일반 게시글 목록·상세·작성·수정·본인 삭제 규칙을 DB 없이 검증한다.
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    // AI작성: 일반 게시글 목록과 페이징 정보가 Mapper 결과에 맞게 반환되는지 확인한다.
    void getPostListReturnsPostsAndPageMeta() {
        PostSearchCondition condition = new PostSearchCondition();
        PostDto post = post(1L, 10L);
        when(postMapper.selectPostListCount(condition)).thenReturn(1);
        when(postMapper.selectPostList(condition)).thenReturn(List.of(post));

        PostListResult result = postService.getPostList(condition);

        assertThat(result.getPostList()).containsExactly(post);
        assertThat(result.getPageMeta().getTotal()).isEqualTo(1);
        assertThat(condition.getOffset()).isZero();
        assertThat(condition.getLimit()).isEqualTo(10);
    }

    @Test
    // AI작성: 작성자가 아닌 회원의 일반 상세 조회는 조회수를 증가시킨 뒤 게시글을 반환하는지 확인한다.
    void getPostDetailIncreasesViewCountForDifferentMember() {
        PostDto post = post(1L, 10L);
        when(postMapper.increaseViewCount(1L, 20L)).thenReturn(1);
        when(postMapper.selectPostDetail(1L)).thenReturn(post);

        PostDto result = postService.getPostDetail(1L, 20L);

        assertThat(result).isSameAs(post);
        verify(postMapper).increaseViewCount(1L, 20L);
        verify(postMapper).selectPostDetail(1L);
    }

    @Test
    // AI작성: 유효한 카테고리의 게시글을 저장하고 생성된 게시글 번호를 반환하는지 확인한다.
    void writePostReturnsGeneratedPostId() {
        PostDto post = post(11L, 10L);
        post.setCategoryId(2L);
        when(postMapper.countCategoryById(2L)).thenReturn(1);
        when(postMapper.insertPost(post)).thenReturn(1);

        Long postId = postService.writePost(post);

        assertThat(postId).isEqualTo(11L);
        verify(postMapper).insertPost(post);
    }

    @Test
    // AI작성: 없는 카테고리의 게시글 작성은 저장하지 않고 INVALID_CATEGORY로 처리하는지 확인한다.
    void writePostRejectsInvalidCategory() {
        PostDto post = post(null, 10L);
        post.setCategoryId(999L);
        when(postMapper.countCategoryById(999L)).thenReturn(0);

        assertThatThrownBy(() -> postService.writePost(post))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_CATEGORY"));
        verify(postMapper, never()).insertPost(post);
    }

    @Test
    // AI작성: 유효한 카테고리와 수정 내용이 전달되면 게시글 번호를 반환하는지 확인한다.
    void editPostReturnsPostId() {
        PostEditRequest request = editRequest(2L);
        when(postMapper.countCategoryById(2L)).thenReturn(1);
        when(postMapper.editPost(1L, request)).thenReturn(1L);

        Long postId = postService.editPost(1L, request);

        assertThat(postId).isEqualTo(1L);
        verify(postMapper).editPost(1L, request);
    }

    @Test
    // AI작성: 수정 대상이 없으면 POST_NOT_FOUND로 처리하는지 확인한다.
    void editPostThrowsWhenPostDoesNotExist() {
        PostEditRequest request = editRequest(2L);
        when(postMapper.countCategoryById(2L)).thenReturn(1);
        when(postMapper.editPost(999L, request)).thenReturn(0L);

        assertThatThrownBy(() -> postService.editPost(999L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("POST_NOT_FOUND"));
    }

    @Test
    // AI작성: 로그인 회원이 작성자이면 일반 게시글 삭제를 수행하는지 확인한다.
    void deletePostAllowsWriter() {
        PostDto post = post(1L, 10L);
        when(postMapper.selectPostDetail(1L)).thenReturn(post);
        when(postMapper.deletePost(1L, 10L)).thenReturn(1);

        postService.deletePost(1L, 10L);

        verify(postMapper).deletePost(1L, 10L);
    }

    @Test
    // AI작성: 작성자가 아닌 로그인 회원은 일반 게시글을 삭제할 수 없는지 확인한다.
    void deletePostRejectsDifferentMember() {
        PostDto post = post(1L, 10L);
        when(postMapper.selectPostDetail(1L)).thenReturn(post);

        assertThatThrownBy(() -> postService.deletePost(1L, 20L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("POST_DELETE_FORBIDDEN");
                    assertThat(exception.getStatus().value()).isEqualTo(403);
                });
        verify(postMapper, never()).deletePost(1L, 20L);
    }

    private PostDto post(Long postId, Long writerId) {
        PostDto post = new PostDto();
        post.setPostId(postId);
        post.setWriterId(writerId);
        post.setTitle("테스트 게시글");
        post.setContent("테스트 내용");
        return post;
    }

    private PostEditRequest editRequest(Long categoryId) {
        PostEditRequest request = new PostEditRequest();
        request.setCategoryId(categoryId);
        request.setTitle("수정 제목");
        request.setContent("수정 내용");
        return request;
    }
}
