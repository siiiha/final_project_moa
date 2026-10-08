package com.nightstudy.moa.post.mapper;

import com.nightstudy.moa.post.dto.PostDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {
    int insertPost(PostDto postDto);

}

