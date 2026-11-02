package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.dto.AdminCommentListItemResponse;
import com.nightstudy.moa.admin.dto.AdminCommentListResult;
import com.nightstudy.moa.admin.dto.AdminCommentSearchCondition;
import com.nightstudy.moa.admin.mapper.AdminCommentMapper;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 관리자 댓글 조회 위임과 관리자 소프트 삭제 결과를 검증한다.
class AdminCommentServiceImplTest {

    @Mock
    private CommentService commentService;

    @Mock
    private AdminCommentMapper adminCommentMapper;

    @InjectMocks
    private AdminCommentServiceImpl adminCommentService;

    @Test
    // AI작성: 전체 댓글 개수로 페이징을 계산하고 현재 페이지 목록을 조회한다.
    void getAdminCommentListReturnsItemsAndPageMeta() {
        AdminCommentSearchCondition condition = new AdminCommentSearchCondition();
        condition.setPage(2);
        condition.setSize(10);
        AdminCommentListItemResponse comment = new AdminCommentListItemResponse();
        comment.setCommentId(11L);
        when(adminCommentMapper.selectAdminCommentCount(condition)).thenReturn(11);
        when(adminCommentMapper.selectAdminCommentList(condition)).thenReturn(List.of(comment));

        AdminCommentListResult result = adminCommentService.getAdminCommentList(condition);

        assertThat(result.getComments()).containsExactly(comment);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(2);
        assertThat(condition.getOffset()).isEqualTo(10);
        assertThat(condition.getLimit()).isEqualTo(10);
    }

    @Test
    // AI작성: 조회 결과가 없으면 불필요한 목록 SQL을 실행하지 않는다.
    void getAdminCommentListReturnsEmptyWithoutListQuery() {
        AdminCommentSearchCondition condition = new AdminCommentSearchCondition();
        when(adminCommentMapper.selectAdminCommentCount(condition)).thenReturn(0);

        AdminCommentListResult result = adminCommentService.getAdminCommentList(condition);

        assertThat(result.getComments()).isEmpty();
        verify(adminCommentMapper, never()).selectAdminCommentList(condition);
    }

    @Test
    // AI작성: 관리자 댓글 조회가 기존 댓글 조회 결과를 그대로 재사용하는지 확인한다.
    void getCommentListDelegatesToCommentService() {
        CommentSearchCondition condition = new CommentSearchCondition();
        CommentListResult expected = new CommentListResult(
                List.of(),
                PageMeta.of(1, 20, 0),
                0
        );
        when(commentService.getCommentList(1L, condition)).thenReturn(expected);

        CommentListResult result =
                adminCommentService.getCommentList(1L, condition);

        assertThat(result).isSameAs(expected);
        verify(commentService).getCommentList(1L, condition);
    }

    @Test
    // AI작성: 관리자 댓글 삭제 결과가 한 건이면 예외 없이 완료되는지 확인한다.
    void deleteCommentCompletesWhenOneCommentIsUpdated() {
        when(adminCommentMapper.deleteComment(1L, 10L)).thenReturn(1);

        adminCommentService.deleteComment(1L, 10L);

        verify(adminCommentMapper).deleteComment(1L, 10L);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 999",
            "2, 10"
    })
    // AI작성: 없거나 이미 삭제됐거나 다른 게시글에 속한 댓글은 모두 COMMENT_NOT_FOUND로 처리한다.
    void deleteCommentReturnsNotFound(Long postId, Long commentId) {
        when(adminCommentMapper.deleteComment(postId, commentId)).thenReturn(0);

        assertThatThrownBy(() ->
                adminCommentService.deleteComment(postId, commentId)
        ).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("COMMENT_NOT_FOUND");
            assertThat(exception.getStatus().value()).isEqualTo(404);
        });
        verify(adminCommentMapper).deleteComment(postId, commentId);
    }
}
