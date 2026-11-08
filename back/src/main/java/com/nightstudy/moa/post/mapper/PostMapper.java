package com.nightstudy.moa.post.mapper;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {
    List<PostCategoryDto> selectPostCategories();
    int insertPost(PostDto postDto);

    // AI작성: 게시글 첨부 정보와 표시 순서를 저장합니다.
    int insertPostImage(@Param("postId") Long postId, @Param("imageUrl") String imageUrl,
                        @Param("sortOrder") int sortOrder);

    List<PostDto> selectPostList(PostSearchCondition condition);

    PostDto selectPostDetail(Long postId);

    int selectPostListCount(PostSearchCondition condition);

    int editPost(
            @Param("postId") Long postId,
            @Param("loginMemberId") Long loginMemberId,
            @Param("request") PostEditRequest request
    );

    int countCategoryById(
            @Param("categoryId") Long categoryId
    );
    int increaseViewCount(
            @Param("postId") Long postId,
            @Param("loginMemberId") Long loginMemberId
    );

    int deletePost(
            @Param("postId") Long postId,
            @Param("loginMemberId") Long loginMemberId
    );

    //게시글 번호에 해당하는 게시글의 존재 여부를 확인
    int countPostById(Long postId);

    // AI작성: 댓글 등록 트랜잭션 동안 게시글 삭제가 동시에 처리되지 않도록 활성 게시글 행을 잠급니다.
    Long selectActivePostIdForUpdate(@Param("postId") Long postId);

}

