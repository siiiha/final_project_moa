package com.nightstudy.moa.saving_group.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter                 // 계모임 이미지는 필수가 아니기 때문에 @NoArgsConstructor로 생성하고, setter를 사용하여 입력 요소에 맞는 각 필드 값을 내부적으로 전달
public class SavingGroupCreationRequest {
    private String groupName;       // 계모임 이름
    private Long groupId;           // 모임 ID
    private Long regionId;        // 활동 지역 ID
    private Long memberId;     // 모임장(계주) 회원 ID
    private int maxMembers;         // 모임원 정원
    private String description;     // 계모임 소개 글
    private String groupType;       // 계모임 종류 (순번계 or 친목계)
    private BigDecimal duesAmount;  // 정기 회비
    private LocalDate duesDay;      // 정기 회부 납부일
    private MultipartFile groupImage;   // 계모임 이미지 파일 (선택 사항)
    private String groupImagePath;       // 실제 DB에 저장될 파일 경로
}
