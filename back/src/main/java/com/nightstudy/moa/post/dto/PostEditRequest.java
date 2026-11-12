package com.nightstudy.moa.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PostEditRequest {
    @NotNull(message = "카테고리는 필수입니다.")
    @Positive(message = "카테고리 값이 올바르지 않습니다.")
    private Long categoryId;
    @NotBlank(message = "제목은 필수입니다.")
    // AI작성: 한글 채움 문자(U+3164)만 입력한 제목 수정을 막는다.
    @Pattern(regexp = "(?s).*[^\\s\\u3164].*", message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;
    @Size(max = 5000, message = "내용은 5,000자 이하로 입력해주세요.")
    private String content;

    // AI작성: API 직접 호출도 포함해 유니코드 공백만으로 된 본문 저장을 차단한다.
    @AssertTrue(message = "내용은 필수입니다.")
    public boolean isContentNotBlank() {
        // AI작성: String.isBlank()이 인식하지 못하는 한글 채움 문자(U+3164)도 빈 내용으로 처리한다.
        return content != null && content.matches("(?s).*[^\\s\\u3164].*");
    }

    // AI작성: 이미지 편집 요청에서 유지할 첨부 ID와 표시 순서를 받습니다.
    private List<@NotNull @Positive Long> retainedImageIds;
}
