# API 使用示例

本文档提供 HBTI Coach API 的实际使用示例，帮助开发者快速集成和使用。

## 目录

- [认证流程](#认证流程)
- [用户注册和登录](#用户注册和登录)
- [健康评估](#健康评估)
- [健康计划](#健康计划)
- [每日追踪](#每日追踪)
- [AI 教练对话](#ai-教练对话)
- [错误处理](#错误处理)

## 基础信息

**Base URL**: `http://localhost:8080/api/v1`

**认证方式**: Cookie-based (HttpOnly)

**Content-Type**: `application/json`

## 认证流程

### 1. 用户注册

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecureP@ssw0rd",
    "confirmPassword": "SecureP@ssw0rd"
  }' \
  -c cookies.txt
```

**响应** (201 Created):
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com"
}
```

注意：认证 Cookie 会自动设置，保存在 `cookies.txt` 中。

### 2. 用户登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecureP@ssw0rd"
  }' \
  -c cookies.txt
```

### 3. 刷新令牌

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -b cookies.txt \
  -c cookies.txt
```

### 4. 登出

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -b cookies.txt
```

## 用户注册和登录

### JavaScript/Fetch 示例

```javascript
// 注册
async function register(email, password) {
  const response = await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include', // 重要：包含 Cookie
    body: JSON.stringify({
      email,
      password,
      confirmPassword: password
    })
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return await response.json();
}

// 登录
async function login(email, password) {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ email, password })
  });
  
  if (!response.ok) {
    throw new Error('登录失败');
  }
  
  return await response.json();
}

// 检查认证状态
async function checkAuth() {
  const response = await fetch('/api/v1/profile', {
    credentials: 'include'
  });
  
  return response.ok;
}
```

## 健康评估

### 完成 HBTI 评估

```bash
# 1. 获取 HBTI 定义
curl -X GET http://localhost:8080/api/v1/hbti/definition \
  -b cookies.txt

# 2. 提交评估答案
curl -X POST http://localhost:8080/api/v1/hbti/assessments \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "answers": [
      {"questionId": "Q001", "selectedOptionId": "OPT_A"},
      {"questionId": "Q002", "selectedOptionId": "OPT_C"},
      {"questionId": "Q003", "selectedOptionId": "OPT_B"}
      // ... 更多答案
    ]
  }'
```

**响应** (201 Created):
```json
{
  "assessmentId": "a1b2c3d4-...",
  "completedAt": "2026-09-18T10:30:00Z",
  "scores": {
    "H": 75,
    "B": 65,
    "T": 80,
    "I": 70
  },
  "primaryType": "H",
  "secondaryType": "T"
}
```

### 获取历史评估

```bash
curl -X GET "http://localhost:8080/api/v1/hbti/assessments?page=0&size=10" \
  -b cookies.txt
```

### JavaScript 示例

```javascript
async function submitAssessment(answers) {
  const response = await fetch('/api/v1/hbti/assessments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ answers })
  });
  
  if (!response.ok) {
    throw new Error('评估提交失败');
  }
  
  return await response.json();
}

async function getAssessmentHistory() {
  const response = await fetch('/api/v1/hbti/assessments', {
    credentials: 'include'
  });
  
  return await response.json();
}
```

## 健康计划

### 创建体重计划

```bash
curl -X POST http://localhost:8080/api/v1/plans/weight \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "currentWeight": 75.5,
    "targetWeight": 70.0,
    "heightCm": 175,
    "targetDate": "2027-01-01",
    "targetCaloriesPerDay": 1800
  }'
```

**响应** (201 Created):
```json
{
  "planId": "plan-123",
  "currentWeight": 75.5,
  "targetWeight": 70.0,
  "weeklyDeficit": 3850,
  "expectedWeeklyLoss": 0.55,
  "projectedEndDate": "2026-12-28",
  "status": "ACTIVE"
}
```

### 获取当前计划

```bash
curl -X GET http://localhost:8080/api/v1/plans/weight/current \
  -b cookies.txt
```

### 更新计划

```bash
curl -X PUT http://localhost:8080/api/v1/plans/weight/plan-123 \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "targetWeight": 68.0,
    "targetCaloriesPerDay": 1700
  }'
```

## 每日追踪

### 记录体重

```bash
curl -X POST http://localhost:8080/api/v1/tracking/weight \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "weightKg": 74.8,
    "recordedAt": "2026-09-18T08:00:00Z",
    "notes": "早餐前测量"
  }'
```

### 记录饮食

```bash
curl -X POST http://localhost:8080/api/v1/tracking/meals \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "mealType": "BREAKFAST",
    "description": "燕麦粥、香蕉、牛奶",
    "calories": 350,
    "recordedAt": "2026-09-18T08:30:00Z"
  }'
```

### 记录运动

```bash
curl -X POST http://localhost:8080/api/v1/tracking/exercises \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "exerciseType": "RUNNING",
    "durationMinutes": 30,
    "caloriesBurned": 300,
    "recordedAt": "2026-09-18T07:00:00Z",
    "notes": "晨跑5公里"
  }'
```

### 获取每日汇总

```bash
curl -X GET "http://localhost:8080/api/v1/tracking/daily/2026-09-18" \
  -b cookies.txt
```

**响应**:
```json
{
  "date": "2026-09-18",
  "weight": {
    "value": 74.8,
    "change": -0.3
  },
  "nutrition": {
    "totalCalories": 1650,
    "targetCalories": 1800,
    "remaining": 150
  },
  "exercise": {
    "totalMinutes": 45,
    "totalCaloriesBurned": 400
  },
  "netCalories": 1250
}
```

### JavaScript 示例

```javascript
async function trackWeight(weightKg, notes = '') {
  const response = await fetch('/api/v1/tracking/weight', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({
      weightKg,
      recordedAt: new Date().toISOString(),
      notes
    })
  });
  
  return await response.json();
}

async function getDailySummary(date) {
  const response = await fetch(`/api/v1/tracking/daily/${date}`, {
    credentials: 'include'
  });
  
  return await response.json();
}
```

## AI 教练对话

### 发送消息（流式响应）

```bash
curl -X POST http://localhost:8080/api/v1/coach/chat \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -b cookies.txt \
  -d '{
    "message": "我今天感觉很累，该怎么调整？"
  }' \
  --no-buffer
```

**响应** (Server-Sent Events):
```
event: token
data: {"text":"根据"}

event: token
data: {"text":"你的"}

event: token  
data: {"text":"情况"}

event: done
data: {"conversationId":"conv-123"}
```

### JavaScript 流式处理示例

```javascript
async function chatWithCoach(message, onToken, onDone, onError) {
  try {
    const response = await fetch('/api/v1/coach/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      credentials: 'include',
      body: JSON.stringify({ message })
    });

    if (!response.ok) {
      throw new Error('Chat request failed');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      
      if (done) break;
      
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 保留不完整的行

      for (const line of lines) {
        if (line.startsWith('event: token')) {
          continue;
        }
        if (line.startsWith('data: ')) {
          const data = JSON.parse(line.slice(6));
          
          if (data.text) {
            onToken(data.text);
          } else if (data.conversationId) {
            onDone(data.conversationId);
          }
        }
      }
    }
  } catch (error) {
    onError(error);
  }
}

// 使用示例
chatWithCoach(
  "我今天感觉很累，该怎么调整？",
  (token) => {
    // 显示每个 token
    console.log('Token:', token);
    document.getElementById('response').textContent += token;
  },
  (conversationId) => {
    console.log('对话完成:', conversationId);
  },
  (error) => {
    console.error('错误:', error);
  }
);
```

### React 示例

```jsx
function CoachChat() {
  const [message, setMessage] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSend = async () => {
    setLoading(true);
    setResponse('');

    await chatWithCoach(
      message,
      (token) => setResponse(prev => prev + token),
      (conversationId) => {
        setLoading(false);
        console.log('对话ID:', conversationId);
      },
      (error) => {
        setLoading(false);
        alert('发送失败: ' + error.message);
      }
    );
  };

  return (
    <div>
      <textarea
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="输入你的问题..."
        disabled={loading}
      />
      <button onClick={handleSend} disabled={loading}>
        {loading ? '发送中...' : '发送'}
      </button>
      <div className="response">
        {response}
        {loading && <span className="cursor">▋</span>}
      </div>
    </div>
  );
}
```

### 获取对话历史

```bash
curl -X GET "http://localhost:8080/api/v1/coach/conversations?page=0&size=20" \
  -b cookies.txt
```

## 错误处理

### 标准错误响应

所有错误响应遵循统一格式：

```json
{
  "timestamp": "2026-09-18T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "验证失败",
  "path": "/api/v1/auth/register",
  "errors": [
    {
      "field": "email",
      "message": "邮箱格式无效"
    }
  ]
}
```

### 常见 HTTP 状态码

| 状态码 | 含义 | 常见原因 |
|--------|------|---------|
| 400 | Bad Request | 请求参数验证失败 |
| 401 | Unauthorized | 未登录或令牌过期 |
| 403 | Forbidden | 无权访问该资源 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（如邮箱已存在） |
| 429 | Too Many Requests | 超出速率限制 |
| 500 | Internal Server Error | 服务器内部错误 |

### JavaScript 错误处理示例

```javascript
async function apiRequest(url, options = {}) {
  const defaultOptions = {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    }
  };

  try {
    const response = await fetch(url, { ...defaultOptions, ...options });
    
    if (!response.ok) {
      const error = await response.json();
      
      // 处理特定错误
      switch (response.status) {
        case 401:
          // 重定向到登录页
          window.location.href = '/login';
          break;
        case 429:
          throw new Error('请求过于频繁，请稍后再试');
        case 500:
          throw new Error('服务器错误，请联系管理员');
        default:
          throw new Error(error.message || '请求失败');
      }
    }
    
    return await response.json();
  } catch (error) {
    console.error('API 请求失败:', error);
    throw error;
  }
}

// 使用示例
try {
  const data = await apiRequest('/api/v1/profile');
  console.log('用户信息:', data);
} catch (error) {
  alert(error.message);
}
```

## 速率限制

API 实施了速率限制以防止滥用：

| 端点 | 限制 |
|------|------|
| 认证端点 (`/api/v1/auth/*`) | 5 次/分钟 |
| 教练对话 (`/api/v1/coach/chat`) | 10 次/分钟 |
| 其他端点 | 60 次/分钟 |

超出限制时会返回 `429 Too Many Requests`。

**响应头**：
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1694999999
```

## 完整工作流程示例

### 新用户完整流程

```javascript
async function completeUserFlow() {
  try {
    // 1. 注册
    await register('newuser@example.com', 'SecureP@ss123');
    console.log('✓ 注册成功');

    // 2. 完成 HBTI 评估
    const assessment = await submitAssessment([
      { questionId: 'Q001', selectedOptionId: 'OPT_A' },
      // ... 更多答案
    ]);
    console.log('✓ 评估完成，类型:', assessment.primaryType);

    // 3. 创建体重计划
    const plan = await fetch('/api/v1/plans/weight', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        currentWeight: 75,
        targetWeight: 70,
        heightCm: 175,
        targetDate: '2027-01-01',
        targetCaloriesPerDay: 1800
      })
    }).then(r => r.json());
    console.log('✓ 计划创建，ID:', plan.planId);

    // 4. 记录第一天数据
    await trackWeight(75, '初始体重');
    console.log('✓ 体重已记录');

    // 5. 与教练对话
    await chatWithCoach(
      '我刚创建了健康计划，有什么建议吗？',
      (token) => process.stdout.write(token),
      (id) => console.log('\n✓ 对话完成:', id),
      (err) => console.error('✗ 对话失败:', err)
    );

    console.log('\n✓ 完整流程完成！');
  } catch (error) {
    console.error('✗ 流程失败:', error);
  }
}
```

## 测试工具

### Postman Collection

导入此 JSON 到 Postman：

```json
{
  "info": {
    "name": "HBTI Coach API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/v1/auth/register",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"test@example.com\",\n  \"password\": \"Test123!\",\n  \"confirmPassword\": \"Test123!\"\n}"
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    }
  ]
}
```

## 下一步

- 查看 [OpenAPI 文档](http://localhost:8080/swagger-ui.html)（应用运行时）
- 阅读 [架构文档](./architecture/hbti-coach-architecture.md)
- 参考 [前端实现](../web/src/) 获取更多示例
