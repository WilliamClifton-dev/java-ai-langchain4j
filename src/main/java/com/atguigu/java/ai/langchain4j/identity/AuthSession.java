package com.atguigu.java.ai.langchain4j.identity;

public record AuthSession(
        RegisteredAccount account,
        IssuedAccessToken accessToken,
        IssuedRefreshToken refreshToken
) {
}
