# 项目状态与证据边界（更新于 2026-10-29）

> 本文件替代旧版“生产就绪 / A+ / 100% 覆盖”状态报告。它记录仓库能证明的事实，不把本地测试结论扩展成平台认证。

## 当前结论

项目达到“核心功能与 L1 公共测试版证据就绪”：后端、前端、离线 AI/RAG 评估和发布脚本已在仓库中；受控生产或公开生产仍需要真实环境证据。

## 已在本地验证

| 项目 | 当前证据 |
|---|---|
| 后端 | `mvn test`：185 tests，0 failures，0 errors，2 skipped |
| 前端 | `npm test -- --run`：34 tests passed；生产构建成功 |
| 数据库 | H2 完整迁移通过；独立 Compose MySQL 从空库成功迁移至 V17 |
| AI/RAG 离线评估 | Offline runner 通过 4 项定向测试；校验 manifest、prompt hash、检索容量和安全边界 |
| 容器交付 | Compose smoke 通过：4 个服务健康、readiness 与同源代理正常，backend/web 非 root 运行 |
| 依赖审计 | npm audit：0 vulnerabilities；OSV runtime：0 项阻塞发现、2 项有代码路径依据的 Spring 例外，见审计报告与[例外清单](../config/security/osv-reachability-exceptions.json) |
| 备份恢复 | fresh-volume restore 通过；比对 21 张表，0 行差异，MySQL restore 用时 23.083 秒 |
| 回滚 | 无效候选在切流前被拒绝（6.606 秒）；已知良好镜像 readiness 于 12.561 秒后恢复 |
| 性能 | k6 L1 脚本已提供；本次尚未运行，不引用容量和延迟结论 |

后端 2 项跳过分别是 OpenAPI schema dump 需要显式系统属性，以及真实模型 smoke 需要显式启用外部测试；均非失败。

## 尚未声称已完成

- 本次本地未运行 k6、真实模型和公开部署；执行后才可引用 p50/p95/p99、错误率和首 token 延迟。
- 公开部署还缺 TLS、托管密钥、异地加密备份、告警、镜像 digest 和平台侧 SLO 证据。
- Spring Boot 3.5.16 已到支持周期末；升级评估存在，但 Boot 4 迁移尚未完成。当前 Spring Framework 两项 OSV 例外依赖具体路由/渲染前提，例外有期限并需复核，不能视作框架仍受支持。
- 真实模型的人工质量评分、模型版本、首 token 延迟和成本尚无报告。

## 状态分级

| 等级 | 含义 | 当前状态 |
|---|---|---|
| 开发 | 功能实现、单元/集成测试 | 已达到 |
| L1 公共测试版 | 离线评估、可复现脚本、容器健康检查 | 仓库门禁和本地 Compose smoke 已通过；固定环境发布证据仍需逐次生成 |
| 受控生产 | 固定环境部署、备份恢复、告警和指标报告 | 待执行 |
| 公开生产 | TLS、密钥托管、异地备份、镜像证明和持续 SLO | 未声称 |

## 下一步验收

1. 在隔离的固定环境执行 [RELEASE_EVIDENCE.md](RELEASE_EVIDENCE.md) 中的 k6 和真实模型评估命令；按发布频率重复恢复与回滚演练。
2. 每份报告记录 commit SHA、环境规格、配置摘要和运行时间。
3. 仅将报告中的实际数字同步到 README、简历和面试材料。
4. 每次候选发布记录 Compose、k6、恢复和回滚报告，并保留对应 CI artifact。
