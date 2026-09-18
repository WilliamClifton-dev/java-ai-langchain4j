package com.atguigu.java.ai.langchain4j.identity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserAccountMapper {

    @Insert("""
            INSERT INTO user_account (
                id, normalized_email, password_hash, status, created_at, updated_at
            ) VALUES (
                #{id}, #{normalizedEmail}, #{passwordHash}, #{status}, #{createdAt}, #{updatedAt}
            )
            """)
    int insert(UserAccount account);

    @Select("""
            SELECT id, normalized_email, password_hash, status, created_at, updated_at
            FROM user_account
            WHERE id = #{id}
            """)
    Optional<UserAccount> findById(String id);

    @Select("""
            SELECT id, normalized_email, password_hash, status, created_at, updated_at
            FROM user_account
            WHERE normalized_email = #{normalizedEmail}
            """)
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
}
