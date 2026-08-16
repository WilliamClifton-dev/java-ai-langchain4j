# HBTI Coach 学习指南

## 项目概述

HBTI Coach 是基于 Spring Boot 3.5.16 和 LangChain4j 1.0.0-beta3 构建的个性化体重管理助手。项目展示了如何将确定性业务规则与 AI 能力安全结合。

**核心理念**：AI 负责解释和对话，代码负责计算、授权和数据完整性。

## 技术栈

- **后端框架**：Spring Boot 3.5.16
- **AI 集成**：LangChain4j 1.0.0-beta3
- **数据库**：MySQL 8（主存储）+ H2（测试）
- **缓存/限流**：Redis 7
- **持久化**：MyBatis 3.0.5 + Flyway
- **安全**：Spring Security + JWT
- **API 文档**：Springdoc OpenAPI 2.8.17
- **构建工具**：Maven
- **容器化**：Docker + Docker Compose

## 架构设计

### 模块化单体

项目采用模块化单体架构，单一部署单元，模块间通过 Java 接口通信：

```
identity → profile → assessment → planning → tracking
                                      ↓           ↓
knowledge ─────────────────────────→ coach ←─────┘
```

**核心模块**：

1. **identity** - 账户、凭证、JWT 令牌、刷新令牌轮换
2. **profile** - 用户资料、安全筛查、成人资格
3. **assessment** - HBTI 评估定义、提交、评分
4. **planning** - BMI/BMR/TDEE 计算、目标范围、计划版本
5. **tracking** - 每日指标、营养、训练、周回顾
6. **coach** - 对话、流式响应、授权工具
7. **knowledge** - 审核知识、词法检索、引用元数据

### 数据架构

**11 个 Flyway 迁移**：

- V1: 会话和消息表
- V2: 身份表（账户、刷新令牌）
- V3: 资料和安全筛查表
- V4: HBTI 定义（冻结版本 1.0.0）
- V5: HBTI 评估结果表
- V6: 体重计划表
- V7: 每日跟踪表
- V8: 周回顾表
- V9: 知识表
- V10: 审计事件表
- V11: 审计请求关联与匿名失败事件升级（MySQL/H2 Java 迁移；逐步检查元数据以支持 DDL 中断后重试）

**关键约束**：

- 所有用户拥有的表包含所有权路径
- 公共标识符使用 UUID
- 幂等键使用 SHA-256 摘要
- 不可变数据：评估定义、计划版本、周回顾

### 安全边界

**认证流程**：

1. 密码使用 BCrypt（cost 12）
2. 访问 JWT：15 分钟 TTL，HS256 签名
3. 刷新令牌：30 天 TTL，SHA-256 摘要存储，家族轮换
4. Cookie 交付：secure, HTTP-only, same-site
5. CSRF 保护：double-submit token

**授权模型**：

- 每个受保护的命令接收已认证的用户 ID
- SQL 查询包含所有权谓词（WHERE user_id = ?）
- 工具调用使用服务器派生的上下文，不接受用户提供的所有者

### 确定性与 AI 边界

**代码必须拥有**：

- 成人资格和安全路由
- HBTI 评分（与原型 JavaScript 一致）
- BMI/BMR/TDEE 计算
- 目标范围（永不低于 BMR）
- 授权、验证、持久化、限流

**AI 可以拥有**：

- 解释、反思性问题、支持性措辞
- 在明确允许的读取工具中选择
- 建议计划调整（保持草稿直到验证）

### 授权 AI 工具

6 个类型化工具：

1. 读取活动计划
2. 读取每日摘要
3. 读取周回顾
4. 写入每日指标
5. 写入营养日志
6. 写入训练日志

**安全特性**：

- 服务器派生的调用上下文（绑定 JWT subject）
- 写入幂等键：服务器 nonce + 工具名称 + 规范参数
- 流式调用：显式注册，取消时清理
- 有界错误码，无异常详情

## 关键实现模式

### 幂等性模式

```java
@Transactional
public Result createIdempotent(Command cmd) {
    userAccountMapper.lockAccountRow(userId);
    String keyHash = sha256(cmd.canonicalKey());
    Result existing = mapper.findByKey(userId, keyHash);
    if (existing != null) {
        if (existing.payloadHash().equals(sha256(cmd.payload()))) {
            return existing; // 重放
        }
        throw new IdempotencyConflictException(); // 键重用
    }
    // 验证、持久化、提交
}
```

### 流式弹性

- 5 秒首 token 超时
- 30 秒总超时
- 最多 5 个并发流
- 熔断器：3 次连续失败 → 30 秒打开
- 客户端断开：取消会话，释放许可，移除工具授权

### Redis 短期状态

- 登录与模型请求使用带 TTL 的共享固定窗口计数器
- Redis key 只包含命名空间和 SHA-256 摘要，不包含原始用户标识或幂等键
- HBTI 提交使用 30 秒短租约；完成后的幂等结果仍以 MySQL 为准
- 仅缓存可从 MySQL 重建的公开 HBTI 定义，TTL 为 1 小时
- Redis 故障时登录/模型准入拒绝，租约绕过，定义读取回源 MySQL

### 可观测性与审计

- `RequestCorrelationFilter` 只接受安全字符和 64 字符以内的 `X-Request-ID`，否则生成 UUID
- `logback-spring.xml` 使用 Logback 内置 `JsonEncoder`，MDC 和 key-value 字段进入逐行 JSON
- `AuditEventService` 只保留白名单详情，限制嵌套深度、集合数量、字符串和 JSON 总长度
- 审计覆盖注册、登录成功/失败、刷新、令牌复用、退出和计划激活；审计失败不改变业务结果
- `CoachMetrics` 记录流终态、首段延迟、SSE 文本段、SSE 事件和六个工具结果
- 指标标签只能来自固定集合，不能使用用户 ID、request ID、原始 URL 或异常文本
- liveness 只回答进程是否存活；readiness 同时检查 MySQL 和 Redis 是否可接流量，诊断详情只对 `ACTUATOR_ADMIN` 开放

### 知识检索

- 词法评分（中文汉字 bigram + 标准化字母数字）
- SQL 过滤：PUBLISHED 版本 + 精确 locale
- 有界：最多 500 个候选，0.20 匹配阈值，最多 5 个段落
- 每个段落包含完整出处元数据

## 开发工作流

### 本地开发

```bash
# 启动 MySQL 和 Redis
docker-compose up -d mysql redis

# 设置环境变量
export MYSQL_PASSWORD=your-password
export MINIMAX_API_KEY=your-api-key

# 运行应用
mvn spring-boot:run

# API 文档
open http://localhost:8080/doc.html
```

### 测试

```bash
# 运行所有测试（H2 + mock models）
mvn test

# 运行特定测试
mvn -Dtest=HbtiScoringEngineTest test

# 外部模型冒烟测试
export RUN_EXTERNAL_TESTS=true
mvn -Dtest=ExternalModelSmokeTest test
```

### 数据库迁移

```bash
# 检查迁移状态
mvn flyway:info

# 验证迁移
mvn flyway:validate
```

## 学习路径

### 第 1 周：基础架构

1. **认证流程** - `identity/` 包
   - 阅读 `AuthenticationService.java`
   - 理解刷新令牌轮换 (`RefreshTokenService.java`)
   - 查看 `V2__create_identity_tables.sql`

2. **所有权模型** - `profile/` 包
   - `ProfileService.java` 如何验证所有者
   - SQL 中的所有权谓词模式

3. **确定性计算** - `planning/` 包
   - `HealthCalculator.java` 中的 BMI/BMR/TDEE
   - `TargetRangePolicy.java` 中的安全约束

### 第 2 周：AI 集成

1. **LangChain4j 集成** - `coach/` 包
   - `CoachChatService.java` 中的 AI Agent 配置
   - `CoachStreamingService.java` 中的流式响应

2. **授权工具** - `coach/tool/` 包
   - `CoachToolProvider.java` 中的工具注册
   - `CoachToolAuthorizationTest.java` 中的安全测试
   - `CoachToolContext.java` 中的服务器派生上下文

3. **提示工程** - `src/main/resources/prompts/hbti/`
   - `core.txt` - 核心系统提示
   - `scenes/` - 场景特定提示

### 第 3 周：高级特性

1. **知识检索** - `knowledge/` 包
   - `ReviewedKnowledgeRetriever.java` 中的词法检索
   - `KnowledgeIngestionService.java` 中的版本管理

2. **流式弹性** - `coach/streaming/` 包
   - `ModelCircuitBreaker.java` 中的熔断器
   - `CoachRateGuard.java` 中的限流
   - `CoachStreamingService.java` 中的超时处理

3. **审计和可观测性** - `common/audit/` 包
   - `AuditEventService.java` 中的敏感字段清洗
   - `RequestCorrelationFilter.java` 中的请求关联与 MDC 生命周期
   - `CoachMetrics.java` 中的低基数 Micrometer 指标
   - `DatabaseHealthIndicator.java` 中的健康检查
   - `HealthProbeTest.java` 中的 readiness/liveness 故障语义

## 常见问题

### Q: 为什么使用 MySQL 而不是 MongoDB？

**A**: 关系数据（用户、评估、计划）需要事务性保证和外键约束。ADR-002 详细说明了这一决策。

### Q: 为什么幂等键使用哈希而不是明文？

**A**: 防止键泄露敏感信息（email、答案内容）。使用 SHA-256 摘要。

### Q: 为什么工具不接受用户提供的 owner 参数？

**A**: 防止提示注入。服务器从已验证的 JWT subject 派生所有者，永不信任模型输出。

### Q: 为什么目标范围永不低于 BMR？

**A**: 安全约束。过低的卡路里目标可能有害。`CONSERVATIVE_ENERGY_RANGE_V1` 策略强制执行。

### Q: 如何添加新的 AI 工具？

**A**: 
1. 在 `coach/tool/` 创建带有 `@Tool` 注解的方法
2. 工具通过 `CoachToolContext` 获取服务器绑定的所有者
3. 调用应用服务（已包含授权）
4. 在 `CoachToolAuthorizationTest` 添加测试
5. 更新工具计数断言

### Q: 生产部署需要什么？

**A**: 
- `AUTH_SECURE_COOKIES=true`
- 强 `AUTH_SIGNING_KEY`（>= 32 字节）
- MySQL 强密码
- HTTPS/TLS 终止
- 监控和警报
- 备份/恢复过程
- 负载测试证据
- 安全审查

## 架构决策记录

重要的 ADR：

- **ADR-001**: 直接进入 HBTI Coach（跳过医疗版本）
- **ADR-002**: 使用 MySQL 作为主存储
- **ADR-004**: HBTI 评分与原型兼容
- **ADR-005**: 健康计算版本和假设
- **ADR-006**: 计划生命周期和状态转换
- **ADR-007**: 每日跟踪事实和重试语义
- **ADR-008**: 确定性周回顾规则
- **ADR-009**: 授权 AI 工具边界
- **ADR-010**: 词法知识检索（非向量）
- **ADR-011**: 工具安全模型
- **ADR-012**: Redis 仅保存有界短期或可重建状态
- **ADR-013**: 有界、厂商中立的日志、指标、审计与健康探针
- **ADR-014**: 账户数据生命周期与活跃 JWT 校验
- **ADR-015**: 采用跨仓库 HBTI 研究与开发协议。V1 不可变，连续维度优先，
  类型码仅作辅助表达；HBTI 只调整表达、重点和监测，不决定热量、安全、
  治疗或高风险运动；黄金样例只证明软件一致性，不证明科学有效性

查看 `docs/decisions/` 获取完整详情。

## 下一步

1. **完成前端闭环**（Task 21）- 每日记录、周回顾和流式教练
2. **交付验证**（Task 22）- 前端 CI、容器健康冒烟和 Compose 证据
3. **评估套件**（Task 23）- 负载测试、备份恢复、回滚和发布门禁
4. **最终交接**（Task 24）- 架构对账、学习材料和模型交接

## 资源

- [架构文档](docs/architecture/hbti-coach-architecture.md)
- [部署指南](docs/DEPLOYMENT.md)
- [API 文档](http://localhost:8080/doc.html)
- [任务计划](tasks/plan.md)
