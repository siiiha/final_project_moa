package com.nightstudy.moa.post.mapper;

import com.nightstudy.moa.post.dto.PostDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {
    int insertPost(PostDto postDto);

    //카테고리 개수를 조회
    int countCategoryById(int categoryId);

}

