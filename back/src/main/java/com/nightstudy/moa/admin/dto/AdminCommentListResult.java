package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// AI작성: 관리자 전체 댓글 목록과 페이징 정보를 함께 반환한다.
public class AdminCommentListResult {
    private final List<AdminCommentListItemResponse> comments;
    private final PageMeta pageMeta;
}
