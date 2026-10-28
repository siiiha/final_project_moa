package com.nightstudy.moa.comment.mapper;

import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.nightstudy.moa.comment.dto.CommentDto;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {
    int insertComment(CommentDto commentDto);
    // AI작성: 삭제되지 않은 댓글 단건 조회는 이 메서드로 통일합니다.
    CommentDto selectCommentById(Long commentId);

    int selectCommentCountByPostId(Long postId);

    // AI작성: 화면 표시용으로 원댓글과 대댓글을 모두 포함한 개수를 조회합니다.
    int selectTotalCommentCountByPostId(Long postId);

    List<CommentResponse> selectCommentListByPostId(
            CommentSearchCondition condition
    );

    // AI작성: 현재 페이지의 원댓글 번호들에 해당하는 대댓글을 한 번에 조회합니다.
    List<CommentResponse> selectRepliesByParentIds(
            @Param("postId") Long postId,
            @Param("parentIds") List<Long> parentIds
    );

    int updateComment(
            @Param("postId") Long postId,
            @Param("commentId") Long commentId,
            @Param("loginMemberId") Long loginMemberId,
            @Param("request") CommentEditRequest request
    );

    int deleteComment(
            @Param("postId") Long postId,
            @Param("commentId") Long commentId,
            @Param("loginMemberId") Long loginMemberId
    );
}
