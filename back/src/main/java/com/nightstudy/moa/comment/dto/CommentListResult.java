package com.nightstudy.moa.comment.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentListResult {
    private final List<CommentResponse> comments;
    private final PageMeta pageMeta;
}
