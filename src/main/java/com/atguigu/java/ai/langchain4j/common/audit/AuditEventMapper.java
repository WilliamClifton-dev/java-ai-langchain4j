package com.atguigu.java.ai.langchain4j.common.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditEventMapper {

    @Insert("""
            INSERT INTO audit_event (
                event_type, user_id, remote_address, request_id, success, details
            ) VALUES (
                #{eventType}, #{userId}, #{remoteAddress}, #{requestId}, #{success}, #{details}
            )
            """)
    void insert(@Param("eventType") String eventType,
                @Param("userId") String userId,
                @Param("remoteAddress") String remoteAddress,
                @Param("requestId") String requestId,
                @Param("success") boolean success,
                @Param("details") String details);
}
