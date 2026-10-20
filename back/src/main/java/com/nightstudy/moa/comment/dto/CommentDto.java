package com.nightstudy.moa.comment.dto;

import jakarta.validation.constraints.NotBlank;
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
    @NotNull(message = "회원 번호는 필수입니다.")
    private Long memberId;
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
