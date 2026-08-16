# HBTI Coach

HBTI Coach 是一个基于 Spring Boot 与 LangChain4j 的个性化体重管理助手后端。它把 HBTI 行为倾向、确定性健康计算、版本化计划、每日执行记录、每周复盘和带授权工具的 AI 教练组织在一个模块化单体中。

HBTI 目前是探索性的行为倾向评估，不是医学诊断、疾病筛查或已经验证的生物学代谢分型。

## 当前能力

- 六类体重管理对话场景及独立 Prompt
- 动态注入日期和场景规则的 HBTI Agent
- 按 `conversationId` 隔离的 MySQL 聊天记忆
- 带 Bean Validation 的 REST API
- 统一的参数校验错误结构
- JWT 登录、刷新令牌轮换与用户资源归属
- 版本化 HBTI 中英文题目、确定性评分和原型金标准测试
- 带幂等提交、当前结果和历史记录的 HBTI 测评 API
- 版本化 BMI/BMR/TDEE、目标范围和计划激活流程
- 每日体重/营养/训练记录与确定性七日复盘
- 六个服务器授权的 AI 读写工具、审核知识检索和引用
- 带首段/总超时、并发上限和熔断器的 SSE 流式教练
- Redis 共享限流、短租约和可重建公开定义缓存
- JSON 请求关联日志、关键流程审计、Micrometer 指标和独立健康探针
- 不访问外部模型或数据库的默认测试套件
- 可显式启用的真实模型 Smoke Test

尚未完成：Web 前端、负载/AI 评测、备份恢复和回滚演练。账户数据导出/删除、OpenAPI 路径契约和可重复执行的 OSV 依赖门禁已经实现。当前版本仍是 L1 公共测试版建设阶段，不是已完成生产验收或企业级认证的产品。

## 技术栈

- Java 17
- Spring Boot 3.5.16
- LangChain4j 1.0.0-beta3
- MySQL 8、MyBatis 和 Flyway
- Ollama 或 MiniMax OpenAI 兼容接口

## 配置

默认启用 `minimax` Profile：

```powershell
$env:MINIMAX_API_KEY = "your-api-key"
$env:MYSQL_PASSWORD = "your-local-password"
mvn spring-boot:run
```

使用本地 Ollama：

```powershell
$env:APP_PROFILE = "local"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
$env:OLLAMA_MODEL_NAME = "qwen:latest"
$env:MYSQL_PASSWORD = "your-local-password"
mvn spring-boot:run
```

MySQL 默认连接 `jdbc:mysql://localhost:3306/hbti_coach`，默认用户名为 `root`，但必须显式提供 `MYSQL_PASSWORD`。可通过 `MYSQL_URL` 和 `MYSQL_USERNAME` 覆盖地址与用户；生产环境不能使用默认凭据或提交真实密钥。

## API

### 发送消息

```http
POST /api/v1/coach/messages
Content-Type: application/json
```

```json
{
  "conversationId": "conversation-1",
  "scene": "GENERAL_CHAT",
  "message": "怎么开始减脂？"
}
```

支持的 `scene`：

- `GENERAL_CHAT`
- `PLAN_GENERATION`
- `DAILY_CHECKIN`
- `WEEKLY_REVIEW`
- `HBTI_INTERPRETATION`
- `SAFETY_SCREENING`

计划、打卡和周报通过六个服务器授权工具调用确定性应用服务。模型文本本身仍不是已保存的业务事实；只有工具事务成功返回后，对应记录才算提交。

## 测试

默认测试使用 H2 的 MySQL 兼容模式，不连接真实模型或外部数据库：

```powershell
mvn test
```

显式运行真实模型 Smoke Test：

```powershell
$env:RUN_EXTERNAL_TESTS = "true"
$env:MINIMAX_API_KEY = "your-api-key"
mvn -Dtest=ExternalModelSmokeTest test
```

## Prompt 结构

```text
src/main/resources/prompts/hbti/
├─ core.txt
└─ scenes/
   ├─ general-chat.txt
   ├─ plan-generation.txt
   ├─ daily-checkin.txt
   ├─ weekly-review.txt
   ├─ hbti-interpretation.txt
   └─ safety-screening.txt
```

核心 Prompt 每次调用都会发送；场景 Prompt 只加载当前请求所选场景。Prompt 仓库会在启动时拒绝空文件。

## 文档

- [AI 模型接力交接文档](docs/AI_HANDOFF.md)
- [现行目标架构](docs/architecture/hbti-coach-architecture.md)
- [历史演进方案](docs/architecture/xiaozhi-to-hbti-coach-architecture.md)
- [ADR-001：直接进入 HBTI Coach](docs/decisions/ADR-001-start-hbti-coach-directly.md)
- [ADR-002：使用 MySQL 作为主存储](docs/decisions/ADR-002-use-mysql-as-primary-store.md)
