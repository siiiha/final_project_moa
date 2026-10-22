package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.mapper.AdminPostMapper;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
// AI작성: DB 없이 관리자 게시글 Service의 조회·페이징·삭제 예외 처리를 검증한다.
class AdminPostServiceImplTest {

    @Mock
    private AdminPostMapper adminPostMapper;

    @InjectMocks
    private AdminPostServiceImpl adminPostService;

    @Test
    // AI작성: 전체 건수에 맞춰 페이징 값을 계산하고 현재 페이지 게시글을 반환하는지 확인한다.
    void getPostListReturnsPostsAndPageMeta() {
        PostSearchCondition condition = new PostSearchCondition();
        condition.setPage(2);
        condition.setSize(10);
        PostDto post = new PostDto();
        post.setPostId(15L);

        when(adminPostMapper.selectPostCount(condition)).thenReturn(15);
        when(adminPostMapper.selectPostList(condition)).thenReturn(List.of(post));

        PostListResult result = adminPostService.getPostList(condition);

        assertThat(result.getPostList()).containsExactly(post);
        assertThat(result.getPageMeta().getPage()).isEqualTo(2);
        assertThat(result.getPageMeta().getSize()).isEqualTo(10);
        assertThat(result.getPageMeta().getTotal()).isEqualTo(15);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
        assertThat(condition.getOffset()).isEqualTo(10);
        assertThat(condition.getLimit()).isEqualTo(10);
    }

    @Test
    // AI작성: 검색 결과가 없으면 목록 SQL을 실행하지 않고 빈 목록을 반환하는지 확인한다.
    void getPostListReturnsEmptyListWithoutListQuery() {
        PostSearchCondition condition = new PostSearchCondition();
        when(adminPostMapper.selectPostCount(condition)).thenReturn(0);

        PostListResult result = adminPostService.getPostList(condition);

        assertThat(result.getPostList()).isEmpty();
        assertThat(result.getPageMeta().getTotal()).isZero();
        verify(adminPostMapper, never()).selectPostList(condition);
    }

    @Test
    // AI작성: 존재하는 게시글은 관리자 상세 조회 결과로 그대로 반환하는지 확인한다.
    void getPostDetailReturnsPost() {
        PostDto post = new PostDto();
        post.setPostId(1L);
        when(adminPostMapper.selectPostDetail(1L)).thenReturn(post);

        PostDto result = adminPostService.getPostDetail(1L);

        assertThat(result).isSameAs(post);
        // AI작성: 관리자 상세 조회는 일반 게시글의 조회수 증가 Mapper를 갖거나 호출하지 않는다.
        verify(adminPostMapper).selectPostDetail(1L);
        verifyNoMoreInteractions(adminPostMapper);
    }

    @Test
    // AI작성: 존재하지 않거나 삭제된 게시글 상세 조회는 POST_NOT_FOUND로 처리하는지 확인한다.
    void getPostDetailThrowsWhenPostDoesNotExist() {
        when(adminPostMapper.selectPostDetail(999L)).thenReturn(null);

        assertThatThrownBy(() -> adminPostService.getPostDetail(999L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("POST_NOT_FOUND");
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    // AI작성: 관리자 게시글 삭제 결과가 한 건이면 예외 없이 완료되는지 확인한다.
    void deletePostCompletesWhenOnePostIsUpdated() {
        when(adminPostMapper.deletePost(1L)).thenReturn(1);

        adminPostService.deletePost(1L);

        verify(adminPostMapper).deletePost(1L);
    }

    @Test
    // AI작성: 삭제 대상이 없거나 이미 삭제됐다면 POST_NOT_FOUND로 처리하는지 확인한다.
    void deletePostThrowsWhenPostDoesNotExist() {
        when(adminPostMapper.deletePost(999L)).thenReturn(0);

        assertThatThrownBy(() -> adminPostService.deletePost(999L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("POST_NOT_FOUND");
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    // AI작성: 0 이하의 페이지와 크기는 공통 페이징 기본값으로 정규화되는지 확인한다.
    void getPostListNormalizesInvalidPageValues() {
        PostSearchCondition condition = new PostSearchCondition();
        condition.setPage(-1);
        condition.setSize(0);
        when(adminPostMapper.selectPostCount(condition)).thenReturn(1);
        when(adminPostMapper.selectPostList(condition)).thenReturn(List.of(new PostDto()));

        PostListResult result = adminPostService.getPostList(condition);

        assertThat(result.getPageMeta().getPage()).isEqualTo(1);
        assertThat(result.getPageMeta().getSize()).isEqualTo(10);
        assertThat(condition.getOffset()).isZero();
        assertThat(condition.getLimit()).isEqualTo(10);
    }

    @Test
    // AI작성: DB 접근 오류가 공통 예외 처리기를 통해 내부 구현을 숨긴 500 응답으로 변환되는지 확인한다.
    void databaseFailureIsConvertedToInternalErrorResponse() {
        PostSearchCondition condition = new PostSearchCondition();
        DataAccessResourceFailureException databaseException =
                new DataAccessResourceFailureException("테스트용 DB 연결 오류");
        when(adminPostMapper.selectPostCount(condition)).thenThrow(databaseException);

        assertThatThrownBy(() -> adminPostService.getPostList(condition))
                .isSameAs(databaseException);

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleEtc(databaseException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getError().getMessage())
                .isEqualTo("서버 오류가 발생했습니다.");
    }
}
