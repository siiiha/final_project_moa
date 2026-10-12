package com.nightstudy.moa.post.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PostListDto {
    private List<PostDto> postList;
    private PageMeta pageMeta;
}
