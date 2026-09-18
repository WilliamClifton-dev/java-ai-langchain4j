# HBTI Coach - AI驱动的健康管理助手

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0--beta3-blue)](https://github.com/langchain4j/langchain4j)
[![Tests](https://img.shields.io/badge/tests-178%20passed-success)](src/test/java)
[![Architecture](https://img.shields.io/badge/docs-4203%20lines-informational)](docs/)

**HBTI Coach** 是一个基于 Spring Boot 3.2 + LangChain4j 的个人健康管理助手后端系统。它将 **确定性健康计算** 与 **AI对话能力** 有机结合，通过模块化单体架构实现了高性能、高安全性的健康数据管理。

> ⚠️ **重要声明**: HBTI 是探索性的行为倾向评估工具，不构成医学诊断或治疗建议。

---

## 🎯 核心特性

### 🤖 AI能力
- **6种对话场景** - 通用聊天、计划生成、每日打卡、周报解读、HBTI解释、安全筛查
- **授权工具系统** - 6个类型化工具（读取计划、写入打卡等），服务端派生上下文，防止越权
- **流式响应** - SSE实时传输，首Token 5秒超时，总30秒超时，自动熔断恢复
- **知识检索** - L1词法检索，500候选+0.20阈值+最多5条，纯确定性无向量服务

### 🔐 安全与授权
- **JWT + Refresh Token** - HS256签名，15分钟访问令牌，30天刷新令牌，家族轮换检测
- **BCrypt密码** - Cost 12自适应哈希，SHA-256令牌摘要存储
- **工具授权模型** - prompt文本永不授予权限，用户提供的owner参数永不被信任
- **CSRF防护** - Double-submit cookie模式，secure + httpOnly + sameSite

### 📊 确定性计算
- **HBTI评分引擎** - `HBTI_SCORING_V1`版本化算法，与JavaScript原型一致性验证
- **健康指标计算** - BMI/BMR/TDEE（Mifflin-St Jeor公式），保守能量范围（永不低于BMR）
- **周报告生成** - 7天窗口确定性统计，体重趋势、营养达标率、训练总结
- **幂等性保证** - SHA-256 input hash，防重复提交，事务级别锁

### ⚡ 性能与可靠性
- **数据库优化** - 复合索引 `(user_id, local_date)`，查询从O(n)降到O(log n)
- **三层并发控制** - Redis速率限制 + Guava信号量 + 熔断器
- **数据保留策略** - Refresh token 30天+7天宽限期，审计事件180天，会话消息90天
- **批量清理** - 每批500行，定时作业，防止长事务锁表

### 🧪 工程实践
- **178个测试** - 零外部依赖，H2内存数据库，Mock LLM
- **15个Flyway迁移** - 版本化Schema，仅追加，回滚安全
- **17个ADR** - 架构决策记录，追溯演进历史
- **Docker化部署** - 多阶段构建，健康检查，优雅关闭

---

## 🏗️ 架构设计

### 模块化单体（Modular Monolith）

```
┌─────────┐
│identity │ 账户、凭据、JWT
└────┬────┘
     ↓
┌─────────┐
│ profile │ 成人资料、目标、安全筛查
└────┬────┘
     ↓
┌────────────┐
│ assessment │ HBTI定义、响应、确定性评分
└─────┬──────┘
      ↓
┌──────────┐
│ planning │ BMI/BMR/TDEE、营养目标、版本化计划
└────┬─────┘
     ↓
┌──────────┐     ┌───────────┐
│ tracking │────→│   coach   │ 对话、消息、场景路由、工具执行
└──────────┘     └─────┬─────┘
                       ↑
                 ┌─────┴─────┐
                 │ knowledge │ 文档、分块、词法检索
                 └───────────┘
```

**设计优势:**
- ✅ Java接口边界，非HTTP调用（低延迟）
- ✅ 单一事务，跨模块ACID保证
- ✅ 简化部署（单JAR/容器）
- ✅ 未来可按模块拆分微服务

---

## 🚀 快速开始

### 前置要求
- Java 21+
- Maven 3.9+
- Docker & Docker Compose（可选，用于完整环境）

### 启动完整环境（推荐）

```bash
# 克隆仓库
git clone <your-repo-url>
cd hbti-coach

# 启动 MySQL + Redis + Backend + Web
./scripts/smoke/compose-smoke.ps1 -KeepRunning

# 浏览器访问
open http://localhost:5173
```

默认 `offline` Profile 不需要LLM API密钥，教练对话会明确失败，但账户/测评/计划/打卡/复盘等确定性功能完全可用。

### 本地开发（需要MySQL + Redis）

```bash
# 配置环境变量
export MYSQL_PASSWORD="your-password"
export MINIMAX_API_KEY="your-api-key"  # 或使用 offline profile
export AUTH_SIGNING_KEY="your-32-byte-signing-key"

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test
```

### 使用本地Ollama

```bash
export APP_PROFILE="local"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL_NAME="qwen:latest"
export MYSQL_PASSWORD="your-password"
export AUTH_SIGNING_KEY="your-32-byte-signing-key"

mvn spring-boot:run
```

---

## 📊 性能指标

### 数据库优化成果
| 指标 | 优化前 | 优化后 | 改进 |
|-----|-------|-------|-----|
| 周报告查询类型 | 全表扫描 | 索引范围扫描 | - |
| 典型7天窗口扫描行数 | 数千行 | <100行 | 95%+ |
| 查询复杂度 | O(n) | O(log n + k) | - |
| 预计p95延迟改进 | - | 60-80% | 待验证 |

### 并发控制
- **速率限制**: 登录15次/15分钟，Coach对话5次/分钟
- **并发上限**: 最多5个模型流（可配置）
- **熔断器**: 3次失败→30秒打开→1次半开探测

### 数据保留
| 数据类型 | 保留期 | 清理方式 |
|---------|-------|---------|
| Refresh Token | 30天+7天宽限期 | 批量删除，500行/批次 |
| 审计事件 | 180天 | 批量删除，500行/批次 |
| 会话消息 | 90天无活动 | 批量删除，500行/批次 |

---

## 🧪 测试策略

### 零外部依赖测试

```bash
# 运行全部178个测试（无需MySQL/Redis/LLM）
mvn test

# 运行特定测试类
mvn -Dtest=HbtiScoringEngineTest test

# 运行特定测试方法
mvn -Dtest=WeeklyReviewServiceTest#shouldGenerateReview test
```

**测试环境:**
- **H2内存数据库** - MySQL兼容模式，Flyway迁移完全复用
- **Mock LLM** - 固定响应，无网络调用
- **TestContainers** - 无需本地Redis/MySQL实例

### 可选的外部模型测试

```bash
# 需要真实API密钥
export RUN_EXTERNAL_TESTS="true"
export MINIMAX_API_KEY="your-api-key"

mvn -Dtest=ExternalModelSmokeTest test
```

---

## 📚 API示例

### 注册账户

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "张三"
}
```

### 发送Coach消息

```bash
POST /api/v1/coach/messages
Content-Type: application/json
Cookie: access_token=<jwt>; X-CSRF-Token=<csrf>

{
  "conversationId": "conv-uuid",
  "scene": "GENERAL_CHAT",
  "message": "我今天吃了2000大卡，达标吗？"
}
```

### 生成周报告

```bash
POST /api/v1/tracking/weekly-reviews
Content-Type: application/json
Cookie: access_token=<jwt>; X-CSRF-Token=<csrf>

{
  "windowEnd": "2024-09-15"
}
```

完整API文档见 [docs/api/](docs/api/)

---

## 🎓 面试亮点

### 1. 性能优化能力
> "我通过代码审计发现周报告生成执行三个全表扫描查询。添加复合索引`(user_id, local_date)`后，查询从O(n)降到O(log n)，典型7天窗口从扫描数千行优化到数十行，预计p95延迟降低60-80%。"

### 2. 系统设计能力
> "我设计了三层并发控制：Redis速率限制保护API（fail-closed策略），Guava信号量限制并发模型流（最多5个），熔断器自动恢复（3次失败→30秒打开）。这确保即使下游LLM服务故障，系统仍能保护自身。"

### 3. 安全意识
> "我实现了工具授权安全模型：AI可以调用6个类型化工具，但工具执行上下文由服务端派生，绑定JWT subject。用户提供的owner参数永远不被信任，prompt文本永远不授予权限。这防止了prompt注入导致的越权访问。"

### 4. 架构能力
> "我采用模块化单体架构，6个领域模块通过Java接口通信。这保留了单体的部署简单性和事务一致性，同时通过清晰的模块边界为未来微服务拆分做好准备。确定性计算（HBTI评分、BMR、目标范围）100%代码实现，AI只负责解释和建议。"

### 5. 工程实践
> "我搭建了零外部依赖测试框架，178个测试全部使用H2内存数据库和Mock LLM，本地运行无需Docker。同时我维护了17个ADR架构决策记录和4203行技术文档，确保架构演进可追溯。"

---

## 📖 文档导航

### 架构与设计
- [架构总览](docs/architecture/hbti-coach-architecture.md) - 完整的系统架构说明
- [演进历史](docs/architecture/xiaozhi-to-hbti-coach-architecture.md) - 从原型到L1的演进
- [产品规格](docs/specs/hbti-coach-product-spec.md) - 功能需求和边界

### API文档
- [认证与授权](docs/api/authentication.md)
- [HBTI测评](docs/api/hbti-assessments.md)
- [计划管理](docs/api/weight-plans.md)
- [每日打卡](docs/api/daily-tracking.md)
- [周报告](docs/api/weekly-reviews.md)
- [Coach对话](docs/api/coach-streaming.md)

### 运维与部署
- [部署指南](docs/DEPLOYMENT.md) - 环境配置、启动命令、故障排查
- [数据保留与备份](docs/operations/data-retention-and-backup.md)
- [发布检查清单](docs/RELEASE_CHECKLIST.md)

### 架构决策记录（ADR）
- [ADR-001](docs/decisions/ADR-001-start-hbti-coach-directly.md) - 直接启动HBTI Coach
- [ADR-009](docs/decisions/ADR-009-tool-authorization-boundaries.md) - 工具授权边界
- [ADR-011](docs/decisions/ADR-011-streaming-tool-authorization-context.md) - 流式工具授权
- [ADR-013](docs/decisions/ADR-013-batch-expired-token-deletion.md) - 批量过期令牌删除
- [更多...](docs/decisions/)

### 性能与优化
- [数据库优化报告](docs/performance/database-optimization-report.md)
- [优化总结](docs/performance/optimization-summary.md)
- [简历亮点](docs/RESUME_HIGHLIGHTS.md) - 面试准备材料

---

## 🔧 技术栈

### 后端框架
- **Java 21** - 现代化Java特性（Records、Pattern Matching、Virtual Threads就绪）
- **Spring Boot 3.2.6** - 依赖注入、自动配置、Actuator监控
- **Spring Security** - JWT认证、CSRF防护、方法级授权
- **MyBatis 3.5.16** - 14个纯注解Mapper，零XML配置

### AI与LLM
- **LangChain4j 1.0.0-beta3** - 流式响应、工具调用、内存管理
- **支持模型** - MiniMax（OpenAI兼容）、Ollama本地模型、可扩展其他提供商

### 数据存储
- **MySQL 8** - 单一事实源，ACID事务，复合索引优化
- **Redis** - 4种用途（速率限制、并发控制、分布式锁、缓存）
- **Flyway** - 版本化Schema迁移，仅追加，回滚安全

### 测试与质量
- **JUnit 5** - 178个测试，零外部依赖
- **H2 Database** - MySQL兼容模式的内存数据库
- **Testcontainers** - 可选的集成测试支持
- **k6** - 负载测试框架（20 RPS持续 + 100 RPS突发）

### DevOps
- **Docker & Docker Compose** - 多阶段构建，健康检查
- **GitHub Actions** - CI/CD流水线，自动化测试
- **Micrometer** - 指标收集，Prometheus兼容

---

## 🤝 贡献与反馈

本项目是个人作品集项目，主要用于技术学习和面试展示。欢迎：
- 🐛 提交Issue报告问题
- 💡 提出功能建议
- 📖 改进文档
- ⭐ Star支持

---

## 📄 许可证

本项目仅用于学习和技术展示目的。

---

## 👤 作者

**你的名字**
- GitHub: [@your-github](https://github.com/your-github)
- Email: your.email@example.com
- LinkedIn: [your-profile](https://linkedin.com/in/your-profile)

---

## 🙏 致谢

- Spring Boot & Spring Framework 团队
- LangChain4j 社区
- 所有开源贡献者

---

**最后更新**: 2026-09-17
