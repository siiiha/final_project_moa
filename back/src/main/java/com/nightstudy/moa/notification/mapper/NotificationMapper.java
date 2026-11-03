package com.nightstudy.moa.notification.mapper;

import com.nightstudy.moa.notification.dto.NotificationDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

    // AI작성: 알림을 등록하고 생성된 notificationId를 DTO에 저장합니다.
    int insert(NotificationDto notificationDto);

    NotificationDto findById(
            @Param("notificationId") Long notificationId
    );

    // AI작성: 마지막 알림 번호가 있으면 그보다 오래된 알림을 조회합니다.
    List<NotificationDto> findRecent(
            @Param("receiverId") Long receiverId,
            // 더 보기 조회
            @Param("lastNotificationId") Long lastNotificationId,
            @Param("size") int size
    );

    int countUnread(
            @Param("receiverId") Long receiverId
    );

    // AI작성: 요청한 알림이 해당 회원의 알림인지 확인합니다.
    boolean existsByIdAndReceiverId(
            @Param("notificationId") Long notificationId,
            @Param("receiverId") Long receiverId
    );

    int markRead(
            @Param("notificationId") Long notificationId,
            @Param("receiverId") Long receiverId
    );

    int markAllRead(
            @Param("receiverId") Long receiverId
    );

    // AI작성: 해당 회원이 이미 읽은 알림을 완전히 삭제하고 삭제 건수를 반환합니다.
    int deleteAllRead(
            @Param("receiverId") Long receiverId
    );
}
