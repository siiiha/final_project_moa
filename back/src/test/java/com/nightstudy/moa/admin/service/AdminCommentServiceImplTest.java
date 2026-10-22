package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.mapper.AdminCommentMapper;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    // AI작성: 댓글이 없거나 이미 삭제됐거나 게시글이 다르면 COMMENT_NOT_FOUND로 처리하는지 확인한다.
    void deleteCommentThrowsWhenCommentDoesNotExist() {
        when(adminCommentMapper.deleteComment(1L, 999L)).thenReturn(0);

        assertThatThrownBy(() -> adminCommentService.deleteComment(1L, 999L))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("COMMENT_NOT_FOUND");
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                });
    }
}
