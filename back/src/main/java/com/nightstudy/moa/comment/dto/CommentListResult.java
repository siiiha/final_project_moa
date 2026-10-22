package com.nightstudy.moa.comment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentListResult {
    private final List<CommentResponse> comments;
    // AI작성: 페이징 정보는 ApiResponse.meta로만 반환하고 data 내부의 중복 직렬화를 막습니다.
    @JsonIgnore
    private final PageMeta pageMeta;
    // AI작성: 화면의 댓글 개수 표시에 사용할 원댓글과 대댓글의 전체 개수입니다.
    private final int totalCommentCount;
}
