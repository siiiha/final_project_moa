package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.SavingGroupChatMessage;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SavingGroupChatMapper {
    // AI작성: 계모임 상세의 ID로 STOMP 방 번호를 찾는다.
    @Select("SELECT room_id FROM chat_room WHERE saving_group_id = #{savingGroupId}")
    Long findRoomId(long savingGroupId);



    // AI작성: 새 메시지 알림을 받을 채팅방 참여자를 발신자를 제외하고 조회한다.
    @Select("""
            SELECT member_id
            FROM chat_room_member
            WHERE room_id = #{roomId}
              AND member_id != #{senderId}
            """)
    List<Long> findNotificationReceiverIds(
            @Param("roomId") long roomId,
            @Param("senderId") Long senderId);

    @Insert("INSERT INTO chat_message (room_id, sender_id, content) VALUES (#{roomId}, #{senderId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "messageId")
    void insert(SavingGroupChatMessage message);

    @Select("""
            SELECT c.message_id AS messageId, c.room_id AS roomId, c.sender_id AS senderId,
                   m.nickname AS senderName, c.content, c.created_at AS createdAt
            FROM chat_message c LEFT JOIN members m ON m.member_id = c.sender_id AND m.deleted_at IS NULL
            WHERE c.message_id = #{messageId}
            """)
    SavingGroupChatMessage findMessage(long messageId);

    @Select("""
            <script>
            SELECT recent.message_id AS messageId, recent.room_id AS roomId, recent.sender_id AS senderId,
                   m.nickname AS senderName, recent.content, recent.created_at AS createdAt
            FROM (
                SELECT message_id, room_id, sender_id, content, created_at
                FROM chat_message WHERE room_id = #{roomId}
                <if test="beforeId != null">AND message_id &lt; #{beforeId}</if>
                ORDER BY message_id DESC LIMIT #{size}
            ) recent
            LEFT JOIN members m ON m.member_id = recent.sender_id AND m.deleted_at IS NULL
            ORDER BY recent.message_id
            </script>
            """)
    List<SavingGroupChatMessage> findHistory(
            @Param("roomId") long roomId,
            @Param("beforeId") Long beforeId,
            @Param("size") int size);
}
