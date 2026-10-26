package com.nightstudy.moa.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@Getter
@Setter
public class CommentDto {
    private Long commentId;
    private Long postId;
    // AI작성: 값이 있으면 원댓글 번호를 의미하며, 대댓글은 한 단계만 허용합니다.
    private Long parentId;
    @NotNull(message = "회원 번호는 필수입니다.")
    private Long memberId;
    @NotBlank(message = "내용을 입력해주세요.")
    // AI작성: 댓글과 대댓글이 지나치게 길어지지 않도록 최대 길이를 제한합니다.
    @Size(max = 500, message = "댓글은 500자 이하로 입력해주세요.")
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
