package com.nightstudy.moa.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentEditRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    // AI작성: 댓글 수정 시에도 등록과 동일하게 최대 500자로 제한합니다.
    @Size(max = 500, message = "댓글은 500자 이하로 입력해주세요.")
    private String content;
}
