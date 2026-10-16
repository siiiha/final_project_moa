package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.mapper.CommentMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


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

        if (content == null || content.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "COMMENT_CONTENT_REQUIRED",
                    "댓글 내용을 입력해주세요."
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

        if (postMapper.selectPostDetail(postId) == null){
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
}

