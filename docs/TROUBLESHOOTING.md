# 故障排查指南

本指南帮助你诊断和解决 HBTI Coach 项目中的常见问题。

## 构建和测试问题

### 测试失败：缺少 AUTH_SIGNING_KEY

**症状**：
```
Could not resolve placeholder 'AUTH_SIGNING_KEY' in value "${AUTH_SIGNING_KEY}"
```

**原因**：测试配置需要 `AUTH_SIGNING_KEY` 环境变量。

**解决方案**：
```bash
# Windows PowerShell
$env:AUTH_SIGNING_KEY = "baseline-test-key-padded-to-32-bytes"
mvn test

# Linux/macOS
export AUTH_SIGNING_KEY="baseline-test-key-padded-to-32-bytes"
mvn test
```

### Maven 依赖解析失败

**症状**：
```
Failed to collect dependencies
```

**解决方案**：
```bash
# 清理本地仓库缓存
mvn dependency:purge-local-repository
mvn clean install
```

### H2 数据库兼容性问题

**症状**：测试中出现 SQL 语法错误。

**解决方案**：检查 `src/test/resources/application-test.yml` 中的 H2 配置：
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:hbti_coach;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

## Docker Compose 问题

### Backend 容器启动失败

**症状**：
```
Container hbti-smoke-backend-1 is unhealthy
```

**诊断步骤**：

1. 检查容器日志：
```bash
docker compose logs backend
```

2. 检查健康检查状态：
```bash
docker inspect hbti-smoke-backend-1 | grep -A 10 Health
```

3. 手动测试健康端点：
```bash
curl http://localhost:8080/actuator/health/readiness
```

**常见原因**：

- **MySQL 未就绪**：确保 MySQL 容器健康
- **Redis 连接失败**：检查 Redis 容器状态
- **端口冲突**：确保 8080 端口未被占用
- **内存不足**：为 Docker 分配更多内存（至少 4GB）

### 端口已被占用

**症状**：
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**解决方案**：
```bash
# 查找占用端口的进程
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/macOS

# 使用不同端口
$env:BACKEND_PORT = "8081"
$env:WEB_PORT = "5174"
docker compose up
```

### 数据持久化问题

**症状**：重启后数据丢失。

**解决方案**：
```bash
# 检查卷是否存在
docker volume ls | grep hbti

# 完全清理后重建
docker compose down --volumes
docker compose up --build
```

## 运行时问题

### 数据库连接失败

**症状**：
```
Communications link failure
```

**诊断步骤**：

1. 检查 MySQL 是否运行：
```bash
docker ps | grep mysql
```

2. 验证连接参数：
```bash
mysql -h localhost -u root -p -e "SELECT 1"
```

3. 检查环境变量：
```bash
echo $MYSQL_PASSWORD
```

**解决方案**：
- 确保 `MYSQL_PASSWORD` 已设置
- 检查 MySQL 容器健康状态
- 验证网络连接

### Redis 连接超时

**症状**：
```
io.lettuce.core.RedisCommandTimeoutException
```

**解决方案**：

1. 检查 Redis 状态：
```bash
redis-cli ping
```

2. 调整超时配置（`application.yml`）：
```yaml
spring:
  data:
    redis:
      timeout: 2000ms
      connect-timeout: 500ms
```

### JWT 签名验证失败

**症状**：
```
Invalid JWT signature
```

**原因**：`AUTH_SIGNING_KEY` 不匹配或未设置。

**解决方案**：
- 开发环境：使用 `docker-compose.yml` 中的默认密钥
- 生产环境：设置至少 32 字节的强密钥
```bash
$env:AUTH_SIGNING_KEY = "your-production-key-at-least-32-bytes"
```

## AI/LLM 相关问题

### Offline profile 下教练调用失败

**症状**：教练流式响应返回错误。

**预期行为**：这是正常的！`offline` profile 禁用了 AI 模型。

**解决方案**：

使用真实模型：
```bash
# 使用 MiniMax
$env:APP_PROFILE = "minimax"
$env:MINIMAX_API_KEY = "your-api-key"

# 使用本地 Ollama
$env:APP_PROFILE = "local"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
$env:OLLAMA_MODEL_NAME = "qwen:latest"
```

### 模型响应超时

**症状**：
```
First token timeout after 5000ms
```

**解决方案**：

调整超时配置（`application.yml`）：
```yaml
hbti:
  coach:
    streaming:
      first-token-timeout: PT10S
      total-timeout: PT60S
```

### 熔断器打开

**症状**：
```
Circuit breaker is OPEN
```

**原因**：连续 3 次失败触发熔断器。

**解决方案**：
1. 检查模型服务可用性
2. 等待 30 秒熔断器自动半开
3. 重启应用重置熔断器

## 性能问题

### 启动时间过长

**优化建议**：

1. 使用本地 Maven 仓库镜像
2. 增加 JVM 堆内存：
```bash
export MAVEN_OPTS="-Xmx2g"
```

3. 跳过不必要的测试：
```bash
mvn spring-boot:run -DskipTests
```

### 查询性能问题

**诊断**：

1. 启用 SQL 日志：
```yaml
logging:
  level:
    org.mybatis: DEBUG
```

2. 检查慢查询日志

3. 分析执行计划：
```sql
EXPLAIN SELECT * FROM daily_metric WHERE user_id = ?;
```

**优化建议**：
- 确保索引已创建（参见 migration V13, V14）
- 使用分页查询大结果集
- 考虑添加复合索引

## 安全审计问题

### OSV 审计发现漏洞

**解决方案**：

1. 查看报告：
```bash
cat target/security/osv-report.json
```

2. 更新依赖：
```xml
<properties>
    <netty.version>最新版本</netty.version>
    <tomcat.version>最新版本</tomcat.version>
</properties>
```

3. 重新审计：
```bash
./scripts/security/osv-audit.ps1 -FailOn HIGH
```

## CI/CD 问题

### GitHub Actions 失败

**常见问题**：

1. **缓存问题**：清除 Actions 缓存
2. **依赖下载超时**：重新运行工作流
3. **环境变量缺失**：检查仓库 Secrets 配置

### Compose smoke 测试不稳定

**已知问题**：CI 环境中容器启动时间不一致。

**当前策略**：测试标记为 `continue-on-error: true`，不阻塞主构建。

## 获取帮助

如果以上方法无法解决问题：

1. 检查 [GitHub Issues](https://github.com/WilliamClifton-dev/java-ai-langchain4j/issues)
2. 查看 [架构文档](./architecture/hbti-coach-architecture.md)
3. 查看 [ADR 决策记录](./decisions/)
4. 提交新的 Issue 并附带：
   - 错误日志
   - 环境信息（OS、Java 版本、Docker 版本）
   - 重现步骤
