package com.atguigu.java.ai.langchain4j.knowledge;

/**
 * 知识检索策略常量
 * <p>
 * L1 实现使用确定性本地词法评分（非向量搜索）：
 * - 中文汉字双字符组 + 标准化字母数字术语
 * - SQL 过滤：PUBLISHED 版本 + 精确区域设置
 * - 有界：最多候选数、最小匹配阈值、最大返回结果数
 */
public final class KnowledgeRetrievalPolicy {

    /**
     * 单次检索扫描的最大候选分块数
     * <p>
     * publishedChunks 查询的 LIMIT 值
     */
    public static final int MAX_CANDIDATE_CHUNKS = 500;

    /**
     * 最小匹配分数阈值
     * <p>
     * 计算公式：(匹配术语数) / (查询术语总数)
     * 低于此阈值的候选结果将被过滤
     */
    public static final double MIN_MATCH_SCORE = 0.20;

    /**
     * 单次检索返回的最大结果数
     */
    public static final int MAX_RESULTS = 5;

    /**
     * 查询字符串最大长度限制
     */
    public static final int MAX_QUERY_LENGTH = 500;

    /**
     * 区域设置字符串最大长度限制
     */
    public static final int MAX_LOCALE_LENGTH = 16;

    private KnowledgeRetrievalPolicy() {
        throw new UnsupportedOperationException("Utility class");
    }
}
