package com.nightstudy.moa.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostDto {
    private Long postId;
    private Long writerId; //작성자
    private int categoryId;
    private String title;
    private String content;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //JSTL표시용
    private String createdAtStr;
    private String updatedAtStr;

    // member join해서 가져올 값
    private String writerNickname;


}