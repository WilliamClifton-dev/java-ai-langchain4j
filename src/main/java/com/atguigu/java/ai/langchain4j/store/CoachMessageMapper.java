package com.atguigu.java.ai.langchain4j.store;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
interface CoachMessageMapper {

    @Select("""
            SELECT message_json
            FROM coach_message
            WHERE conversation_id = #{conversationId}
            ORDER BY sequence_no
            """)
    List<String> findMessageJsonByConversationId(String conversationId);

    @Insert("""
            INSERT INTO coach_conversation (id, version, created_at, updated_at)
            VALUES (#{conversationId}, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP(6)
            """)
    int ensureConversation(String conversationId);

    @Select("""
            SELECT id
            FROM coach_conversation
            WHERE id = #{conversationId}
            FOR UPDATE
            """)
    String lockConversation(String conversationId);

    @Delete("DELETE FROM coach_message WHERE conversation_id = #{conversationId}")
    int deleteMessages(String conversationId);

    @Insert("""
            <script>
            INSERT INTO coach_message (conversation_id, sequence_no, message_json)
            VALUES
            <foreach collection="messages" item="message" separator=",">
                (#{conversationId}, #{message.sequenceNo}, #{message.messageJson})
            </foreach>
            </script>
            """)
    int insertMessages(
            @Param("conversationId") String conversationId,
            @Param("messages") List<StoredChatMessage> messages
    );

    @Update("""
            UPDATE coach_conversation
            SET version = version + 1,
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE id = #{conversationId}
            """)
    int incrementVersion(String conversationId);

    @Delete("DELETE FROM coach_conversation WHERE id = #{conversationId}")
    int deleteConversation(String conversationId);
}
