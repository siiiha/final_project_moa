package com.nightstudy.moa.post.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostListResult {
    private List<PostDto> postList;
    private PageMeta pageMeta;
}
