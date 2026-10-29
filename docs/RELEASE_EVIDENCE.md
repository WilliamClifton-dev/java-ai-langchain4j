# 发布与体验取证

把代码存在、自动测试通过和实际运行结果分开记录。`target/` 报告默认不提交；发布说明应引用 CI artifact 或 release artifact，并绑定完整 commit SHA。

## 本地 L1 取证

在 Windows PowerShell、Docker Desktop 和仓库根目录执行：

```powershell
$env:AUTH_SIGNING_KEY = "local-evidence-key-padded-to-at-least-32-bytes"
mvn --batch-mode --no-transfer-progress clean test
npm --prefix web test -- --run
npm --prefix web run build
./scripts/evaluation/run-ai-safety-evaluation.ps1 -DeploymentMode Offline
./scripts/smoke/compose-smoke.ps1 -TimeoutSeconds 300
./scripts/load/run-l1-load.ps1 -ConcurrentSeconds 10 -SustainedSeconds 60 -BurstSeconds 10
./scripts/recovery/test-mysql-restore.ps1
./scripts/release/test-rollback.ps1
./scripts/release/verify-release.ps1 -DeploymentMode Offline -Purpose Evidence
```

报告在 `target/release-evidence/` 与 `target/compose-smoke/`。检查每份报告的 `gitCommit` 是否等于待发布 commit，并一并记录运行主机规格、Docker 版本和时间。k6 的 20 RPS / 100 RPS 是测试负载配置，不代表相同规模的真实用户或容量承诺。

依赖审计会把可达性已核对、但当前框架分支没有修复版本的 advisory 写入 `documented_non_reachable` 例外，并要求在 `config/security/osv-reachability-exceptions.json` 的 `reviewBy` 前复核。例外不会隐藏在报告外，也不覆盖同一依赖的其他漏洞。

## 真实模型评估

离线 AI/RAG 评估不验证真实模型回答质量。真实模型评估需配置模型供应商密钥和固定模型版本，再按 [AI_MODEL_EVALUATION.md](AI_MODEL_EVALUATION.md) 对 manifest 中所有必测场景运行并人工评分。

PowerShell 外部连通性 smoke：

```powershell
$env:RUN_EXTERNAL_TESTS = "true"
$env:MINIMAX_API_KEY = "<secret-from-local-secret-store>"
mvn -Dtest=ExternalModelSmokeTest test
```

此 smoke 只证明模型 API 有响应，不代表安全或产品质量通过。不要将密钥、真实用户输入、健康数据或未经脱敏的对话写入报告。模型证据需匹配当前 `manifestSha256`、`promptBundleSha256`、供应商和精确模型版本；使用 `scripts/evaluation/run-ai-safety-evaluation.ps1 -DeploymentMode ModelEnabled -ProviderEvidence <path>` 验证门禁。

## 指标记录规则

- HTTP：记录测试环境、数据规模、并发/到达率、样本数、p50/p95/p99、错误率和 dropped iterations。
- 模型：记录供应商、精确模型版本、场景、首 token 延迟、总延迟、token 用量和成本边界。
- 数据库：保存 EXPLAIN 输出和代表性数据量；索引存在本身不等于延迟改善。
- 恢复与回滚：记录故障注入点、恢复耗时、数据完整性断言和最终状态。
- 所有数字都应由同一 commit 的机器报告支持，不从目标阈值推导成效。
