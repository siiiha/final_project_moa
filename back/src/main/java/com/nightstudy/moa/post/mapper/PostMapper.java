package com.nightstudy.moa.post.mapper;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {
    int insertPost(PostDto postDto);
    List<PostDto> selectPostList(PostListDto postListDto)

}

