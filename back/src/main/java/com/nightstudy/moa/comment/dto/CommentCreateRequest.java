package com.nightstudy.moa.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI작성: 댓글 작성자 번호는 JWT 인증 정보로만 결정한다.
@Getter
@Setter
public class CommentCreateRequest {
    @NotBlank(message = "내용을 입력해주세요.")
    // AI작성: 한글 채움 문자(U+3164)만 입력한 댓글·대댓글 등록을 막는다.
    @Pattern(regexp = "(?s).*[^\\s\\u3164].*", message = "내용을 입력해주세요.")
    @Size(max = 500, message = "댓글은 500자 이하로 입력해주세요.")
    private String content;
}
