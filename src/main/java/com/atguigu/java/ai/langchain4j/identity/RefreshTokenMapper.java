package com.atguigu.java.ai.langchain4j.identity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {

    @Insert("""
            INSERT INTO refresh_token (
                id, user_id, token_hash, family_id, replaced_by_token_id,
                expires_at, revoked_at, created_at
            ) VALUES (
                #{id}, #{userId}, #{tokenHash}, #{familyId}, #{replacedByTokenId},
                #{expiresAt}, #{revokedAt}, #{createdAt}
            )
            """)
    int insert(RefreshToken token);

    @Select("""
            SELECT id, user_id, token_hash, family_id, replaced_by_token_id,
                   expires_at, revoked_at, created_at
            FROM refresh_token
            WHERE token_hash = #{tokenHash}
            FOR UPDATE
            """)
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    @Select("""
            SELECT id, user_id, token_hash, family_id, replaced_by_token_id,
                   expires_at, revoked_at, created_at
            FROM refresh_token
            WHERE id = #{id}
            """)
    Optional<RefreshToken> findById(String id);

    @Update("""
            UPDATE refresh_token
            SET revoked_at = #{revokedAt},
                replaced_by_token_id = #{replacementId}
            WHERE id = #{id} AND revoked_at IS NULL
            """)
    int markRotated(
            @Param("id") String id,
            @Param("replacementId") String replacementId,
            @Param("revokedAt") Instant revokedAt
    );

    @Update("""
            UPDATE refresh_token
            SET revoked_at = COALESCE(revoked_at, #{revokedAt})
            WHERE user_id = #{userId} AND family_id = #{familyId}
            """)
    int revokeFamily(
            @Param("userId") String userId,
            @Param("familyId") String familyId,
            @Param("revokedAt") Instant revokedAt
    );
}
