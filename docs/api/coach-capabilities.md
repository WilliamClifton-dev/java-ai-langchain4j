# Coach capability endpoint

`GET /api/v1/coach/capabilities` returns the model availability that the web client should show before opening the coach composer.

The endpoint is authenticated and intentionally exposes no provider credentials or internal model configuration.

```json
{
  "available": false,
  "mode": "OFFLINE",
  "message": "当前环境未配置 AI 模型"
}
```

`available=true` is returned for a configured `local` or `minimax` profile. In offline mode the core workflow remains available: profile, HBTI assessment, plan, daily tracking and weekly review do not depend on the coach model.
