import { afterEach, describe, expect, it, vi } from 'vitest';

import { api, resetCsrfTokenForTests } from './http';

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const session = {
  user: { id: 'account-a', email: 'a@example.com' },
  accessExpiresAt: '2099-01-01T00:00:00Z',
};
const unauthorized = () => jsonResponse({ error: { code: 'UNAUTHENTICATED', message: 'Session expired' } }, 401);

describe('protected request and coach stream regressions', () => {
  it('reloads an expired CSRF contract before retrying a rejected write', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'stale' }))
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'INVALID_CSRF_TOKEN' } }, 403))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'current' }))
      .mockResolvedValueOnce(jsonResponse({ record: { id: 'saved' }, replayed: false }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(api.recordDailyMetric({ localDate: '2026-09-08', weightKg: 70 }, 'original-key'))
      .resolves.toMatchObject({ record: { id: 'saved' } });
    const retried = fetchMock.mock.calls[3][1];
    expect(new Headers(retried.headers).get('X-XSRF-TOKEN')).toBe('current');
    expect(new Headers(retried.headers).get('Idempotency-Key')).toBe('original-key');
  });

  it('recovers expired CSRF and access cookies without changing the submitted record', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'old' }))
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'INVALID_CSRF_TOKEN' } }, 403))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'new' }))
      .mockResolvedValueOnce(unauthorized())
      .mockResolvedValueOnce(jsonResponse(session))
      .mockResolvedValueOnce(jsonResponse({ record: { id: 'saved' }, replayed: false }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(api.recordDailyMetric({ localDate: '2026-09-08', weightKg: 70 }, 'same-key'))
      .resolves.toMatchObject({ record: { id: 'saved' } });
    const writes = fetchMock.mock.calls.filter(([path]) => path === '/api/v1/tracking/daily-metrics');
    expect(writes).toHaveLength(3);
    for (const [, init] of writes) {
      expect(new Headers(init.headers).get('Idempotency-Key')).toBe('same-key');
      expect(JSON.parse(init.body)).toEqual({ localDate: '2026-09-08', weightKg: 70 });
    }
  });

  it('never retries an ordinary permission denial as a CSRF error', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'FORBIDDEN' } }, 403));
    vi.stubGlobal('fetch', fetchMock);
    await expect(api.recordDailyMetric({ localDate: '2026-09-08', weightKg: 70 }, 'key'))
      .rejects.toMatchObject({ status: 403, code: 'FORBIDDEN' });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
  afterEach(() => {
    resetCsrfTokenForTests();
    vi.unstubAllGlobals();
  });

  it('refreshes concurrent expired requests once and returns both retried results', async () => {
    let refreshed = false;
    let refreshCount = 0;
    const paths: string[] = [];
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const path = String(input);
      paths.push(path);
      if (path === '/api/v1/auth/csrf') return jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' });
      if (path === '/api/v1/auth/refresh') {
        refreshCount += 1;
        refreshed = true;
        return jsonResponse(session);
      }
      if (path === '/api/v1/auth/session') return jsonResponse(session);
      if (!refreshed) return unauthorized();
      if (path === '/api/v1/profile') return jsonResponse({ userId: 'account-a', currentWeightKg: 70 });
      if (path === '/api/v1/plans/active') return jsonResponse({ id: 'plan-a', status: 'ACTIVE' });
      throw new Error(`Unexpected request ${path}`);
    }));

    const results = await Promise.allSettled([api.getProfile(), api.getActivePlan()]);

    expect(results).toEqual([
      { status: 'fulfilled', value: { userId: 'account-a', currentWeightKg: 70 } },
      { status: 'fulfilled', value: { id: 'plan-a', status: 'ACTIVE' } },
    ]);
    expect(refreshCount).toBe(1);
    expect(paths.filter((path) => path === '/api/v1/profile')).toHaveLength(2);
    expect(paths.filter((path) => path === '/api/v1/plans/active')).toHaveLength(2);
  });

  it('stops after one retry when a refreshed session is still rejected', async () => {
    let refreshCount = 0;
    let profileCount = 0;
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const path = String(input);
      if (path === '/api/v1/auth/csrf') return jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' });
      if (path === '/api/v1/auth/refresh') {
        refreshCount += 1;
        if (refreshCount > 1) throw new Error('Repeated refresh would loop');
        return jsonResponse(session);
      }
      if (path === '/api/v1/auth/session') return jsonResponse(session);
      if (path === '/api/v1/profile') {
        profileCount += 1;
        return unauthorized();
      }
      throw new Error(`Unexpected request ${path}`);
    }));

    await expect(api.getProfile()).rejects.toMatchObject({ status: 401, code: 'UNAUTHENTICATED' });
    expect(refreshCount).toBe(1);
    expect(profileCount).toBe(2);
  });

  it('preserves a rate-limit error delivered before metadata', async () => {
    const event = { type: 'error', code: 'MODEL_RATE_LIMITED', message: 'Try again later', retryable: true };
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(new Response(`event:error\ndata:${JSON.stringify(event)}\n\n`, {
        headers: { 'Content-Type': 'text/event-stream' },
      })));
    const events: unknown[] = [];

    await api.streamCoach({ conversationId: 'conversation-a', scene: 'GENERAL_CHAT', message: 'Hello' }, {
      onEvent: (received) => events.push(received),
    });

    expect(events).toEqual([event]);
  });
});
