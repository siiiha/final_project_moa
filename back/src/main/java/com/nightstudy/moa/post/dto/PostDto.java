package com.nightstudy.moa.post.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

//TODO: 요청 DTO와 응답 DTO 분리할지 고민

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostDto {
    private Long postId;
    private Long writerId; //작성자
    private Long categoryId;
    // AI작성: 게시글 목록과 상세 화면에 카테고리 이름을 표시하기 위한 조회 필드
    private String categoryName;
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //화면 표시용
    private String createdAtStr;
    private String updatedAtStr;

    // member join해서 가져올 값
    private String writerNickname;


}
