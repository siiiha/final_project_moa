package com.nightstudy.moa.smallGroup.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
// AI작성: 인증 작성자와 공통 모임 ID로 공지를 저장하고 생성 키를 전달한다.
@Getter @Setter @AllArgsConstructor
public class SmallGroupNoticeCreateRow {
    private Long noticeId;
    private Long groupId;
    private Long writerId;
    private String title;
    private String content;
}
