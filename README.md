# HBTI Coach

一个基于 Spring Boot 3 + LangChain4j 1.0 的体重管理 AI 教练后端。把 HBTI 行为倾向、确定性健康计算、版本化计划、每日执行、每周复盘与带授权工具的 AI 教练，组织在一个模块化单体中。

> ⚠️ HBTI 目前是**探索性的行为倾向评估**，不是医学诊断、疾病筛查或已经验证的生物学代谢分型。

## 为什么这个项目

我一直想做"AI 在健康场景里真正落地"的样子——不是 demo，而是带工程纪律的实践。所以选了一个有真实用户路径的产品（体重管理），用 AI 解决最难的部分（个性化对话与解读），用确定性服务守住所有不能出错的边界（业务事实、计费逻辑、版本化测评）。

## 工程亮点

- **AI 系统边界**：业务事实（计划、打卡、复盘、测评）**只能由确定性服务**写入；模型通过 6 个**服务器授权工具**触达应用层。模型文本本身永远不是已保存的事实。
- **16 个 ADR（ADR-001 ~ ADR-016）**：每一个技术选型都有书面理由——JWT 轮换、确定性 vs LLM 边界、Redis 临时状态、fail-fast 签名密钥、版本化健康计算等
- **工程硬化（PR #1）**：ADR-016 fail-fast + 4 个契约测试；CI 6/6 通过（Backend / Frontend quality gates、Compose runtime smoke）
- **可复现 L1 公共测试版**：docker-compose 一键起，k6 L1 容量门禁，备份/恢复演练，回滚演练，全部脚本化
- **可观测性**：JSON 请求关联日志、关键流程审计、Micrometer 指标、独立健康探针
- **6 类真实业务对话**：通用聊天 / 计划生成 / 每日打卡 / 每周复盘 / HBTI 解读 / 安全筛查，每类独立 Prompt 与工具集

## 技术栈

Java 17 · Spring Boot 3.5 · LangChain4j 1.0 · MySQL 8 + MyBatis + Flyway · Redis · Ollama / OpenAI 兼容接口 · Docker Compose · k6

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
- 前后端独立镜像、同源 Nginx 代理与可断言的 Compose 健康冒烟
- 版本化 AI/RAG 评估、k6 L1 容量门禁、API 演示数据、备份恢复和回滚演练
- 明确保留期限、每日清理任务、运营 Runbook 和机器可读发布清单

Web 关键流程、容器交付和 Task 23 发布证据已经实现。账户数据导出/删除、OpenAPI 路径契约和可重复执行的 OSV 依赖门禁也已实现。仓库达到可复现的 L1 公共测试版代码与演练标准；真正公开部署仍需提供 TLS、托管密钥、异地加密备份、告警和已发布镜像 digest 的平台证明。它不是企业级/L2 或受监管医疗产品。

## 技术栈

- Java 17
- Spring Boot 3.5.16
- LangChain4j 1.0.0-beta3
- MySQL 8、MyBatis 和 Flyway
- Ollama 或 MiniMax OpenAI 兼容接口

## 配置

无需模型密钥启动完整确定性功能与 Web 界面：

```powershell
./scripts/smoke/compose-smoke.ps1 -KeepRunning
```

浏览器入口是 `http://localhost:5173/`。默认 `offline` Profile 会让教练调用明确失败，但不会影响账户、测评、计划、记录和复盘。详细配置和清理命令见 [部署指南](docs/DEPLOYMENT.md)。



> **本地运行与测试都需要 AUTH_SIGNING_KEY 环境变量。** 该变量至少 32 字节，且不能是 docker-compose.yml 中固定的开发默认值（除非当前 profile 是 offline / local / test）。部署前请按 ADR-016 注入平台密钥。

完整离线 L1 发布证据：

```powershell
./scripts/evaluation/run-ai-safety-evaluation.ps1
./scripts/load/run-l1-load.ps1
./scripts/recovery/test-mysql-restore.ps1
./scripts/release/test-rollback.ps1
./scripts/release/verify-release.ps1 -DeploymentMode Offline -Purpose Evidence
```

直接在主机运行时，应用默认启用 `minimax` Profile：

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
- [L1 发布清单](docs/RELEASE_CHECKLIST.md)
- [数据保留与备份策略](docs/operations/data-retention-and-backup.md)
- [历史演进方案](docs/architecture/xiaozhi-to-hbti-coach-architecture.md)
- [ADR-001：直接进入 HBTI Coach](docs/decisions/ADR-001-start-hbti-coach-directly.md)
- [ADR-002：使用 MySQL 作为主存储](docs/decisions/ADR-002-use-mysql-as-primary-store.md)

