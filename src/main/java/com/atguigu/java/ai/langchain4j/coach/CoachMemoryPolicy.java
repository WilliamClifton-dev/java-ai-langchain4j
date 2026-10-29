package com.atguigu.java.ai.langchain4j.coach;

/**
 * Coach 内存和对话策略常量
 * <p>
 * 定义了 Coach 对话内存窗口大小和内存 ID 限制
 */
public final class CoachMemoryPolicy {

    /**
     * 消息窗口大小（消息数量，非轮次）
     * <p>
     * MessageWindowChatMemory 保留的最大消息数
     * 10 条消息 = 5 轮对话（用户 + 助手各一条）
     */
    public static final int MESSAGE_WINDOW_SIZE = 10;

    /**
     * 内存 ID（对话 ID）最大长度限制
     * <p>
     * 用于 MySqlChatMemoryStore 验证
     */
    public static final int MAX_MEMORY_ID_LENGTH = 128;

    private CoachMemoryPolicy() {
        throw new UnsupportedOperationException("Utility class");
    }
}
