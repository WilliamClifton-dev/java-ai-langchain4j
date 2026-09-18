# 部署最佳实践

本文档提供 HBTI Coach 在不同环境中部署的最佳实践和建议。

## 目录

- [环境配置](#环境配置)
- [安全加固](#安全加固)
- [性能优化](#性能优化)
- [监控和告警](#监控和告警)
- [备份和恢复](#备份和恢复)
- [升级策略](#升级策略)

## 环境配置

### 生产环境最低要求

**硬件**：
- CPU: 2 核心（推荐 4 核心）
- 内存: 4GB（推荐 8GB）
- 磁盘: 20GB（SSD 推荐）

**软件**：
- Java 17 或更高版本
- MySQL 8.0
- Redis 7.x
- Docker 24.x 和 Docker Compose V2（容器化部署）

### 环境变量清单

#### 必需变量

```bash
# 数据库
MYSQL_PASSWORD=<强密码>
MYSQL_URL=jdbc:mysql://mysql-host:3306/hbti_coach?useSSL=true

# 认证
AUTH_SIGNING_KEY=<至少32字节的强密钥>
AUTH_SECURE_COOKIES=true

# Redis
REDIS_URL=redis://redis-host:6379

# AI 模型（根据选择的 profile）
MINIMAX_API_KEY=<your-api-key>  # 如果使用 minimax
# 或
OLLAMA_BASE_URL=http://ollama-host:11434  # 如果使用 local
OLLAMA_MODEL_NAME=qwen:latest
```

#### 推荐变量

```bash
# CORS（根据前端域名配置）
CORS_ALLOWED_ORIGINS=https://your-domain.com

# 应用配置
APP_PROFILE=minimax  # 或 local

# 保留期限（可选，有默认值）
REFRESH_TOKEN_RETENTION_GRACE=P7D
AUDIT_EVENT_RETENTION=P180D

# 性能调优
COACH_MAX_INPUT_TOKENS=8000
COACH_MAX_OUTPUT_TOKENS=1500
```

### Profile 选择

| Profile | 用途 | 要求 |
|---------|------|------|
| `offline` | 本地开发、CI 测试 | 无外部依赖 |
| `local` | 本地开发（带 AI） | 运行 Ollama |
| `minimax` | 生产环境 | MiniMax API 密钥 |

## 安全加固

### 1. 密钥管理

**AUTH_SIGNING_KEY**：
```bash
# 生成安全的签名密钥（至少 32 字节）
openssl rand -base64 32

# 在生产环境中使用密钥管理服务
# - AWS Secrets Manager
# - Azure Key Vault
# - HashiCorp Vault
```

**数据库密码**：
- 使用强密码（至少 16 字符，包含大小写、数字、特殊字符）
- 定期轮换（建议每 90 天）
- 不要在代码或配置文件中硬编码

### 2. 网络安全

**TLS/SSL**：
```bash
# 在反向代理（如 Nginx）中配置 TLS
# 强制 HTTPS 重定向
# 使用 Let's Encrypt 免费证书

# 确保 AUTH_SECURE_COOKIES=true
```

**防火墙规则**：
- 仅允许必要的端口（80, 443）
- 限制数据库和 Redis 访问仅来自应用服务器
- 使用安全组/网络策略

### 3. CORS 配置

```bash
# 生产环境：明确指定允许的源
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com

# 不要在生产环境使用 *
```

### 4. 容器安全

**非 root 用户运行**：
```dockerfile
# Dockerfile 已配置非 root 用户
USER 10001:10001
```

**镜像扫描**：
```bash
# 使用 Trivy 扫描镜像漏洞
trivy image hbti-coach:latest
```

**最小权限原则**：
```yaml
# docker-compose.yml 中不使用 privileged: true
# 限制 capabilities
```

## 性能优化

### 1. JVM 调优

**生产环境 JVM 参数**：
```bash
JAVA_OPTS="-Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/heapdump.hprof \
  -Dspring.profiles.active=minimax"
```

### 2. 数据库优化

**连接池配置**：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 2000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**索引**：
- 项目已包含必要的索引（V13, V14 迁移）
- 定期运行 `ANALYZE TABLE` 更新统计信息
- 监控慢查询日志

### 3. Redis 配置

```yaml
spring:
  data:
    redis:
      timeout: 2000ms
      connect-timeout: 500ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### 4. 应用优化

**速率限制**：
- 使用 Redis 限流（已集成）
- 根据实际负载调整限流规则

**缓存策略**：
- 公开定义数据已缓存
- 监控缓存命中率

## 监控和告警

### 1. 健康检查

**Actuator 端点**：
```bash
# 就绪探针（用于负载均衡器）
curl http://localhost:8080/actuator/health/readiness

# 存活探针（用于容器重启）
curl http://localhost:8080/actuator/health/liveness

# 详细健康信息（需要认证）
curl http://localhost:8080/actuator/health
```

**Kubernetes 配置示例**：
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

### 2. 指标收集

**Micrometer 集成**：
- 应用已集成 Micrometer
- 导出到 Prometheus：
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

**关键指标**：
- JVM 内存使用
- HTTP 请求延迟（P50, P95, P99）
- 数据库连接池使用率
- Redis 连接状态
- 熔断器状态

### 3. 日志管理

**结构化日志**：
- 应用使用 JSON 格式日志
- 包含 `requestId` 用于请求追踪

**日志聚合**：
- 使用 ELK Stack（Elasticsearch, Logstash, Kibana）
- 或云服务（CloudWatch, Azure Monitor）

**日志级别**：
```yaml
logging:
  level:
    root: INFO
    com.atguigu.java.ai.langchain4j: INFO
    org.springframework.security: WARN
```

### 4. 告警规则示例

```yaml
# Prometheus 告警规则
groups:
  - name: hbti-coach
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "高错误率告警"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        annotations:
          summary: "数据库连接池接近耗尽"

      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} > 0
        for: 1m
        annotations:
          summary: "熔断器打开"
```

## 备份和恢复

### 1. 数据库备份

**自动备份脚本**：
```bash
#!/bin/bash
# backup-mysql.sh

BACKUP_DIR=/var/backups/mysql
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="hbti_coach_${DATE}.sql.gz"

# 创建备份
mysqldump -h localhost -u root -p${MYSQL_PASSWORD} \
  --single-transaction \
  --routines \
  --triggers \
  hbti_coach | gzip > ${BACKUP_DIR}/${BACKUP_FILE}

# 上传到远程存储（S3, Azure Blob, etc.）
aws s3 cp ${BACKUP_DIR}/${BACKUP_FILE} s3://your-bucket/backups/

# 保留最近 30 天的备份
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -delete
```

**定时任务**：
```bash
# crontab -e
# 每天凌晨 2 点备份
0 2 * * * /path/to/backup-mysql.sh
```

### 2. 恢复测试

**定期演练**（建议每季度）：
```bash
# 1. 恢复到测试环境
gunzip < backup.sql.gz | mysql -h test-host -u root -p hbti_coach

# 2. 验证数据完整性
# 3. 测试应用功能

# 参考：scripts/recovery/test-mysql-restore.ps1
```

### 3. Redis 持久化

```yaml
# docker-compose.yml
redis:
  command: ["redis-server", "--appendonly", "yes"]
  volumes:
    - redis-data:/data
```

## 升级策略

### 1. 蓝绿部署

```bash
# 1. 部署新版本到绿色环境
docker-compose -f docker-compose.green.yml up -d

# 2. 验证健康检查
curl http://green-host:8080/actuator/health/readiness

# 3. 切换流量（在负载均衡器）
# 4. 监控错误率
# 5. 如有问题，回滚到蓝色环境
```

### 2. 数据库迁移

**零停机迁移原则**：
- 所有迁移必须向后兼容
- 先添加新列/表，再删除旧的
- 使用 Flyway 版本控制

**迁移步骤**：
```bash
# 1. 备份数据库
./backup-mysql.sh

# 2. 部署新版本（Flyway 自动迁移）
docker-compose up -d backend

# 3. 验证迁移
docker-compose logs backend | grep "Flyway"

# 4. 如有问题，回滚
./scripts/release/test-rollback.ps1
```

### 3. 回滚计划

**快速回滚清单**：
1. 保留前一个版本的镜像
2. 准备回滚脚本
3. 数据库迁移使用 `repeatable` 迁移或版本化视图
4. 验证回滚程序（参考 `scripts/release/test-rollback.ps1`）

## Docker Compose 生产部署

### 基础配置

```yaml
# docker-compose.prod.yml
services:
  backend:
    image: hbti-coach:${VERSION}
    restart: always
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    environment:
      SPRING_PROFILES_ACTIVE: minimax
      # 其他环境变量从 .env 文件或密钥管理服务加载
```

### Nginx 反向代理

```nginx
# nginx.conf
upstream backend {
    server backend:8080;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/cert.pem;
    ssl_certificate_key /etc/ssl/private/key.pem;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator/health {
        proxy_pass http://backend;
        access_log off;
    }
}
```

## 清单检查

部署前检查：

- [ ] 所有密钥已设置且安全
- [ ] TLS 证书已配置
- [ ] 防火墙规则已设置
- [ ] 监控和告警已配置
- [ ] 备份策略已实施并测试
- [ ] 日志聚合已配置
- [ ] 健康检查端点可访问
- [ ] 回滚程序已准备
- [ ] 负载测试已通过
- [ ] 安全扫描已通过
- [ ] 文档已更新

## 参考资源

- [部署指南](./DEPLOYMENT.md)
- [故障排查](./TROUBLESHOOTING.md)
- [架构文档](./architecture/hbti-coach-architecture.md)
- [安全更新](./SECURITY_UPDATE_2026_09_18.md)
