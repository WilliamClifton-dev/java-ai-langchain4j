package com.atguigu.java.ai.langchain4j.store;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface CoachConversationOwnershipMapper {

    @Insert("""
            INSERT INTO coach_conversation (id, user_id, version, created_at, updated_at)
            VALUES (#{conversationId}, #{userId}, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                user_id = COALESCE(user_id, VALUES(user_id)),
                updated_at = CURRENT_TIMESTAMP(6)
            """)
    int claim(@Param("conversationId") String conversationId,
              @Param("userId") String userId);

    @Select("""
            SELECT COUNT(*)
            FROM coach_conversation
            WHERE id = #{conversationId} AND user_id = #{userId}
            """)
    int countOwned(@Param("conversationId") String conversationId,
                   @Param("userId") String userId);
}
