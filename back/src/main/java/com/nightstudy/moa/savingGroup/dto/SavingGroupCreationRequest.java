package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 계모임 개설 요청 DTO를 savingGroup 도메인 패키지로 통합한다.
@NoArgsConstructor
@Getter
@Setter
public class SavingGroupCreationRequest {
    @NotBlank(message = "계모임 이름을 입력해 주세요.")
    @Size(max = 100, message = "계모임 이름은 100자 이내로 입력해 주세요.")
    private String groupName;
    private Long groupId;
    private Long regionId;
    @NotBlank(message = "활동 지역을 선택해 주세요.")
    @Size(max = 100, message = "활동 지역을 다시 선택해 주세요.")
    private String regionName;
    // AI작성: 신청자 ID는 클라이언트 입력값이 아닌 JWT 인증 정보로 서버에서 설정한다.
    private Long memberId;
    @NotNull(message = "모집 인원을 입력해 주세요.")
    // AI작성: 개설 신청의 모집 인원은 4~20명으로 제한한다.
    @Min(value = 4, message = "모집 인원은 4명 이상이어야 합니다.")
    @Max(value = 20, message = "모집 인원은 20명 이하여야 합니다.")
    private Integer maxMembers;
    @NotBlank(message = "계모임 소개를 입력해 주세요.")
    @Size(max = 50, message = "계모임 소개는 50자 이내로 입력해 주세요.")
    private String description;
    @NotBlank(message = "계모임 종류를 선택해 주세요.")
    @Pattern(regexp = "친목계|순번계", message = "계모임 종류를 다시 선택해 주세요.")
    private String groupType;
    @NotNull(message = "월 회비를 입력해 주세요.")
    // AI작성: 개설과 수정의 회비 범위를 일치시킨다.
    @DecimalMin(value = "10000", message = "월 회비는 10,000원 이상이어야 합니다.")
    @DecimalMax(value = "1000000", message = "월 회비는 1,000,000원 이하여야 합니다.")
    @Digits(integer = 10, fraction = 0, message = "월 회비는 원 단위 정수로 입력해 주세요.")
    private BigDecimal duesAmount;
    @NotNull(message = "납부일을 선택해 주세요.")
    @Min(value = 1, message = "납부일을 다시 선택해 주세요.")
    @Max(value = 28, message = "납부일을 다시 선택해 주세요.")
    private Integer duesDay;
    private MultipartFile groupImage;
    private String groupImagePath;
}
