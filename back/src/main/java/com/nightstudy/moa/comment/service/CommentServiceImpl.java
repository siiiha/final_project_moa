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
import java.util.Objects;
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
    public CommentDto insertComment(Long postId, Long memberId, String content) {
        if (postMapper.countPostById(postId) == 0) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }

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
    public CommentListResult getCommentList(Long postId,
                                            CommentSearchCondition condition){

        if (postMapper.countPostById(postId) == 0){
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }
        // AI작성: URL로 전달된 게시글 번호를 댓글 조회 조건에 저장합니다.
        condition.setPostId(postId);
        int total = commentMapper.selectCommentCountByPostId(postId);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), total);

        // AI작성: 댓글이 없으면 목록 SQL을 추가로 실행하지 않고 빈 목록을 반환합니다.
        if (total == 0){
            return new CommentListResult(
                    List.of(),
                    pageMeta
            );
        }

        // AI작성: PageMeta가 정규화한 페이징 값을 댓글 조회 조건에 저장합니다.
        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        List<CommentResponse> comments = commentMapper.selectCommentListByPostId(condition);

        return new CommentListResult(
                comments,
                pageMeta
        );
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long postId, Long commentId, CommentEditRequest request) {
        // AI작성: Validation을 거치지 않은 직접 호출도 방어하기 위해 내용을 확인합니다.
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "COMMENT_CONTENT_REQUIRED",
                    "댓글에 내용을 입력해주세요."
            );
        }

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
        int deleteRows = commentMapper.deleteComment(postId, commentId);

        if (deleteRows != 1) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다."
            );
        }
    }
}

