package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 소모임 생성 화면의 필수 입력과 기존 groups/small_group 컬럼 길이를 서버에서 검증한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupCreationRequest {
    // AI작성: 클라이언트 JSON이 아닌 Controller가 JWT RequestAttribute에서 설정하는 내부 전달값이다.
    private Long memberId;

    @NotBlank(message = "소모임 이름을 입력해 주세요.")
    @Size(max = 100, message = "소모임 이름은 100자 이내로 입력해 주세요.")
    private String groupName;

    @NotBlank(message = "활동 지역을 선택해 주세요.")
    @Size(max = 100, message = "활동 지역을 다시 선택해 주세요.")
    private String regionName;

    @NotBlank(message = "관심사 카테고리를 선택해 주세요.")
    @Size(max = 50, message = "관심사 카테고리는 50자 이내로 입력해 주세요.")
    private String likeCategory;

    @NotBlank(message = "소모임 소개를 입력해 주세요.")
    @Size(max = 50, message = "소모임 소개는 50자 이내로 입력해 주세요.")
    private String description;

    @NotNull(message = "정원을 입력해 주세요.")
    @Min(value = 2, message = "정원은 2명 이상이어야 합니다.")
    @Max(value = 20, message = "정원은 20명 이하여야 합니다.")
    private Integer maxMembers;

    @NotNull(message = "만남 일정을 선택해 주세요.")
    @Future(message = "만남 일정은 내일부터 선택할 수 있습니다.")
    private LocalDate meetDate;

    @NotBlank(message = "만남 장소를 입력해 주세요.")
    @Size(max = 40, message = "만남 장소는 40자 이내로 입력해 주세요.")
    private String location;

    // AI작성: Service/Mapper 내부 전달용 필드이며 클라이언트 입력으로 사용하지 않는다.
    private Long regionId;
    private Long groupId;
    private Long smallGroupId;
}
