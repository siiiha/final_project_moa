package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.mapper.CommentMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    @Autowired
    public CommentServiceImpl(
            CommentMapper commentMapper,
            PostMapper postMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
    }

    @Override
    // AI작성: 댓글 저장과 등록 결과 조회를 하나의 작업으로 묶어 중간 실패 시 저장을 롤백합니다.
    @Transactional
    public CommentDto insertComment(Long postId, Long memberId, String content) {
        validatePostExists(postId);

        CommentDto comment = new CommentDto();
        comment.setPostId(postId);
        comment.setMemberId(memberId);
        comment.setContent(content);

        int insertRows = commentMapper.insertComment(comment);

        if (insertRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "COMMENT_CREATE_FAILED",
                    "댓글 등록에 실패했습니다."
            );
        }

        return commentMapper.selectCommentById(comment.getCommentId());
    }

    @Override
    @Transactional
    public CommentDto insertReply(Long postId, Long parentCommentId, Long memberId, String content) {
        validatePostExists(postId);

        CommentDto parentComment = commentMapper.selectCommentById(parentCommentId);

        // AI작성: 다른 게시글의 댓글, 삭제된 댓글, 대댓글에는 답글을 등록할 수 없습니다.
        if (parentComment == null || !Objects.equals(parentComment.getPostId(), postId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "PARENT_COMMENT_NOT_FOUND",
                    "답글을 작성할 댓글을 찾을 수 없습니다."
            );
        }
        if (parentComment.getParentId() != null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "COMMENT_DEPTH_EXCEEDED",
                    "대댓글에는 답글을 작성할 수 없습니다."
            );
        }

        CommentDto reply = new CommentDto();
        reply.setPostId(postId);
        reply.setParentId(parentCommentId);
        reply.setMemberId(memberId);
        reply.setContent(content);

        if (commentMapper.insertComment(reply) != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "COMMENT_CREATE_FAILED",
                    "대댓글 등록에 실패했습니다."
            );
        }

        return commentMapper.selectCommentById(reply.getCommentId());
    }


    @Override
    public CommentListResult getCommentList(Long postId,
                                            CommentSearchCondition condition){
        validatePostExists(postId);
        // AI작성: URL로 전달된 게시글 번호를 댓글 조회 조건에 저장합니다.
        condition.setPostId(postId);
        int total = commentMapper.selectCommentCountByPostId(postId);
        int totalCommentCount = commentMapper.selectTotalCommentCountByPostId(postId);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), total);

        // AI작성: 댓글이 없으면 목록 SQL을 추가로 실행하지 않고 빈 목록을 반환합니다.
        if (total == 0){
            return new CommentListResult(
                    List.of(),
                    pageMeta,
                    totalCommentCount
            );
        }

        // AI작성: PageMeta가 정규화한 페이징 값을 댓글 조회 조건에 저장합니다.
        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        List<CommentResponse> comments = commentMapper.selectCommentListByPostId(condition);

        // AI작성: 전체 댓글은 있지만 요청 페이지가 범위를 벗어난 경우 빈 IN 절 실행을 막습니다.
        if (comments.isEmpty()) {
            return new CommentListResult(
                    List.of(),
                    pageMeta,
                    totalCommentCount
            );
        }

        // AI작성: 현재 페이지의 원댓글에 속한 대댓글을 한 번에 조회해 원댓글별로 묶습니다.
        List<Long> parentIds = comments.stream()
                .map(CommentResponse::getCommentId)
                .toList();
        List<CommentResponse> replies = commentMapper.selectRepliesByParentIds(postId, parentIds);
        Map<Long, List<CommentResponse>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(CommentResponse::getParentId));
        comments.forEach(comment -> comment.setReplies(
                repliesByParentId.getOrDefault(comment.getCommentId(), List.of())
        ));

        return new CommentListResult(
                comments,
                pageMeta,
                totalCommentCount
        );
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long postId, Long commentId, CommentEditRequest request) {
        // AI작성: 삭제된 게시글의 댓글은 수정할 수 없도록 활성 게시글을 확인합니다.
        validatePostExists(postId);

        CommentDto comment = commentMapper.selectCommentById(commentId);

        //댓글 존재 여부와 요청한 게시글에 속한 댓글인지 함께 확인
        //Objects.equals(a, b)는 두 값이 같은지 null까지 안전하게 비교하는 Java 메서드
        if (comment == null || !Objects.equals(comment.getPostId(), postId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다."
            );
        }

        if (comment.getContent().equals(request.getContent())) {
            return comment;
        }

        int updatedRow = commentMapper.updateComment(postId, commentId, request);

        if (updatedRow != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "COMMENT_UPDATE_FAILED",
                    "댓글 수정에 실패했습니다."
            );
        }
        return commentMapper.selectCommentById(commentId);
    }

    @Override
    public void deleteComment (Long postId, Long commentId) {
        // AI작성: 삭제된 게시글의 댓글은 삭제 요청을 처리하지 않도록 활성 게시글을 확인합니다.
        validatePostExists(postId);

        int deleteRows = commentMapper.deleteComment(postId, commentId);

        if (deleteRows != 1) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다."
            );
        }
    }

    // AI작성: 댓글 기능에서 반복되는 활성 게시글 존재 검증을 한 곳에서 처리합니다.
    private void validatePostExists(Long postId) {
        if (postMapper.countPostById(postId) == 0) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }
    }
}

