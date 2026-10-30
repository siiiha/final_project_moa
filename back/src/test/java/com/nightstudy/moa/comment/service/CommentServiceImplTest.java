package com.nightstudy.moa.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.mapper.CommentMapper;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.service.MemberFeatureRestrictionService;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 일반 댓글 등록·조회·수정·삭제와 대댓글 조립 규칙을 DB 없이 검증한다.
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private PostMapper postMapper;

    // AI작성: 댓글 변경 전에 수행되는 CONTENT 이용 제한 검사를 Mock으로 주입한다.
    @Mock
    private MemberFeatureRestrictionService restrictionService;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    // AI작성: 댓글의 앞뒤 공백을 제거해 저장하고 생성된 댓글을 반환하는지 확인한다.
    void insertCommentReturnsCreatedComment() {
        when(postMapper.countPostById(1L)).thenReturn(1);
        doAnswer(invocation -> {
            CommentDto comment = invocation.getArgument(0);
            assertThat(comment.getContent()).isEqualTo("댓글");
            comment.setCommentId(10L);
            return 1;
        }).when(commentMapper).insertComment(any(CommentDto.class));
        CommentDto saved = comment(10L, 1L, null, "댓글");
        when(commentMapper.selectCommentById(10L)).thenReturn(saved);

        CommentDto result = commentService.insertComment(1L, 2L, "  댓글  ");

        assertThat(result).isSameAs(saved);
        verify(commentMapper).insertComment(any(CommentDto.class));
    }

    @Test
    // AI작성: 대댓글에도 일반 댓글과 동일한 앞뒤 공백 제거 규칙이 적용되는지 확인한다.
    void insertReplyTrimsContent() {
        CommentDto parent = comment(10L, 1L, null, "원댓글");
        CommentDto savedReply = comment(11L, 1L, 10L, "대댓글");
        when(postMapper.countPostById(1L)).thenReturn(1);
        when(commentMapper.selectCommentById(10L)).thenReturn(parent);
        doAnswer(invocation -> {
            CommentDto reply = invocation.getArgument(0);
            assertThat(reply.getContent()).isEqualTo("대댓글");
            reply.setCommentId(11L);
            return 1;
        }).when(commentMapper).insertComment(any(CommentDto.class));
        when(commentMapper.selectCommentById(11L)).thenReturn(savedReply);

        CommentDto result = commentService.insertReply(1L, 10L, 2L, "  대댓글  ");

        assertThat(result).isSameAs(savedReply);
    }

    @Test
    // AI작성: 존재하지 않거나 삭제된 게시글에는 댓글을 등록하지 않는지 확인한다.
    void insertCommentRejectsMissingPost() {
        when(postMapper.countPostById(999L)).thenReturn(0);

        assertThatThrownBy(() -> commentService.insertComment(999L, 2L, "댓글"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("POST_NOT_FOUND"));
        verify(commentMapper, never()).insertComment(any(CommentDto.class));
    }

    @Test
    // AI작성: 원댓글과 대댓글을 조회해 부모 댓글 아래에 묶고 전체 댓글 수를 반환하는지 확인한다.
    void getCommentListGroupsReplies() {
        CommentSearchCondition condition = new CommentSearchCondition();
        CommentResponse parent = response(10L, null, "원댓글");
        CommentResponse reply = response(11L, 10L, "대댓글");
        when(postMapper.countPostById(1L)).thenReturn(1);
        when(commentMapper.selectCommentCountByPostId(1L)).thenReturn(1);
        when(commentMapper.selectTotalCommentCountByPostId(1L)).thenReturn(2);
        when(commentMapper.selectCommentListByPostId(condition)).thenReturn(List.of(parent));
        when(commentMapper.selectRepliesByParentIds(1L, List.of(10L)))
                .thenReturn(List.of(reply));

        CommentListResult result = commentService.getCommentList(1L, condition);

        assertThat(result.getComments()).containsExactly(parent);
        assertThat(result.getComments().get(0).getReplies()).containsExactly(reply);
        assertThat(result.getTotalCommentCount()).isEqualTo(2);
        assertThat(result.getPageMeta().getTotal()).isEqualTo(1);
    }

    @Test
    // AI작성: 댓글이 없는 게시글은 목록 SQL과 대댓글 SQL 없이 빈 목록을 반환하는지 확인한다.
    void getCommentListReturnsEmptyList() {
        CommentSearchCondition condition = new CommentSearchCondition();
        when(postMapper.countPostById(1L)).thenReturn(1);
        when(commentMapper.selectCommentCountByPostId(1L)).thenReturn(0);
        when(commentMapper.selectTotalCommentCountByPostId(1L)).thenReturn(0);

        CommentListResult result = commentService.getCommentList(1L, condition);

        assertThat(result.getComments()).isEmpty();
        assertThat(result.getTotalCommentCount()).isZero();
        verify(commentMapper, never()).selectCommentListByPostId(condition);
    }

    @Test
    // AI작성: 수정 댓글의 앞뒤 공백을 제거해 저장한 후 변경된 댓글을 반환하는지 확인한다.
    void updateCommentReturnsUpdatedComment() {
        CommentDto before = comment(10L, 1L, null, "기존 내용");
        CommentDto after = comment(10L, 1L, null, "수정 내용");
        CommentEditRequest request = new CommentEditRequest();
        request.setContent("  수정 내용  ");
        when(postMapper.countPostById(1L)).thenReturn(1);
        when(commentMapper.selectCommentById(10L)).thenReturn(before, after);
        when(commentMapper.updateComment(1L, 10L, 2L, request))
                .thenReturn(1);
        before.setMemberId(2L);
        CommentDto result = commentService.updateComment(1L, 10L, 2L, request);

        assertThat(request.getContent()).isEqualTo("수정 내용");
        assertThat(result.getContent()).isEqualTo("수정 내용");
        verify(commentMapper).updateComment(1L, 10L, 2L, request);    }

    @Test
    // AI작성: 다른 게시글의 댓글 수정 요청은 COMMENT_NOT_FOUND로 처리하는지 확인한다.
    void updateCommentRejectsCommentFromDifferentPost() {
        CommentDto comment = comment(10L, 2L, null, "기존 내용");
        CommentEditRequest request = new CommentEditRequest();
        request.setContent("수정 내용");
        when(postMapper.countPostById(1L)).thenReturn(1);
        when(commentMapper.selectCommentById(10L)).thenReturn(comment);

        assertThatThrownBy(() -> commentService.updateComment(1L, 10L, 2L, request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("COMMENT_NOT_FOUND"));
        verify(commentMapper, never())
                .updateComment(1L, 10L, 2L, request);
    }

    @Test
    // AI작성: 활성 게시글의 댓글 삭제 결과가 한 건이면 정상 완료되는지 확인한다.
    void deleteCommentCompletesWhenOneCommentIsUpdated() {
        when(postMapper.countPostById(1L)).thenReturn(1);
        when(commentMapper.deleteComment(1L, 10L, 2L))
                .thenReturn(1);

        CommentDto comment = comment(10L, 1L, null, "댓글");
        comment.setMemberId(2L);
        when(commentMapper.selectCommentById(10L)).thenReturn(comment);
        commentService.deleteComment(1L, 10L, 2L);

        verify(commentMapper).deleteComment(1L, 10L, 2L);
    }

    @Test
    // AI작성: 없거나 이미 삭제된 댓글은 COMMENT_NOT_FOUND로 처리하는지 확인한다.
    void deleteCommentThrowsWhenCommentDoesNotExist() {
        when(postMapper.countPostById(1L)).thenReturn(1);

        assertThatThrownBy(() -> commentService.deleteComment(1L, 999L, 2L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("COMMENT_NOT_FOUND"));
    }

    private CommentDto comment(
            Long commentId,
            Long postId,
            Long parentId,
            String content
    ) {
        CommentDto comment = new CommentDto();
        comment.setCommentId(commentId);
        comment.setPostId(postId);
        comment.setParentId(parentId);
        comment.setContent(content);
        return comment;
    }

    private CommentResponse response(Long commentId, Long parentId, String content) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(commentId);
        response.setParentId(parentId);
        response.setContent(content);
        return response;
    }
}
