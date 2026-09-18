package com.atguigu.java.ai.langchain4j.common.audit;

public enum AuditEventType {
    ACCOUNT_REGISTERED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    TOKEN_REFRESH,
    TOKEN_REUSE_DETECTED,
    LOGOUT,
    PLAN_ACTIVATED,
    DATA_EXPORT_REQUESTED,
    DATA_DELETION_REQUESTED,
    ACCOUNT_DELETED
}
