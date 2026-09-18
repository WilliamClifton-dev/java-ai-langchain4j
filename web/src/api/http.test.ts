import { afterEach, describe, expect, it, vi } from 'vitest';

import { ApiError, api, resetCsrfTokenForTests } from './http';

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

describe('API client', () => {
  afterEach(() => {
    resetCsrfTokenForTests();
    vi.unstubAllGlobals();
  });

  it('sends browser credentials without exposing an authorization token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ user: { id: 'u1', email: 'user@example.com' } }));
    vi.stubGlobal('fetch', fetchMock);

    await api.getSession();

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/session', expect.objectContaining({
      credentials: 'include',
    }));
    const headers = new Headers(fetchMock.mock.calls[0][1]?.headers);
    expect(headers.has('Authorization')).toBe(false);
  });

  it('loads the server CSRF contract before a mutation', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ user: { id: 'u1', email: 'user@example.com' }, accessExpiresAt: '2026-08-16T12:00:00Z' }));
    vi.stubGlobal('fetch', fetchMock);

    await api.login({ email: 'user@example.com', password: 'correct horse battery staple' });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    const request = fetchMock.mock.calls[1];
    const headers = new Headers(request[1]?.headers);
    expect(request[0]).toBe('/api/v1/auth/login');
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-value');
    expect(headers.get('Content-Type')).toBe('application/json');
  });

  it('preserves the stable backend error code and details', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'Email or password is incorrect',
        details: { field: 'credentials' },
      },
    }, { status: 401 })));

    await expect(api.getSession()).rejects.toMatchObject({
      status: 401,
      code: 'INVALID_CREDENTIALS',
      details: { field: 'credentials' },
    } satisfies Partial<ApiError>);
  });

  it('uses CSRF for profile replacement', async () => {
    const profile = {
      dateOfBirth: '1990-01-01', calculationSex: 'FEMALE' as const, heightCm: 165,
      currentWeightKg: 70, targetWeightKg: 60, activityLevel: 'MODERATE' as const,
      timeZone: 'Asia/Hong_Kong',
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ userId: 'u1', ...profile }));
    vi.stubGlobal('fetch', fetchMock);

    await api.saveProfile(profile);

    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/profile');
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({ method: 'PUT', credentials: 'include' }));
    expect(new Headers(fetchMock.mock.calls[1][1]?.headers).get('X-XSRF-TOKEN')).toBe('csrf-value');
  });

  it('writes an explicit-unit daily metric with CSRF and idempotency', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ record: { id: 'metric-1' }, replayed: false }, { status: 201 }));
    vi.stubGlobal('fetch', fetchMock);

    await api.recordDailyMetric({
      localDate: '2026-08-16', weightKg: 70.2, steps: 8_000,
      activityMinutes: 45, sleepMinutes: 450, sleepQuality: 4,
    }, 'metric-attempt-1');

    const [path, request] = fetchMock.mock.calls[1];
    expect(path).toBe('/api/v1/tracking/daily-metrics');
    expect(new Headers(request?.headers).get('Idempotency-Key')).toBe('metric-attempt-1');
    expect(JSON.parse(request?.body as string)).toMatchObject({ weightKg: 70.2, sleepMinutes: 450 });
  });

  it('generates a weekly review without accepting an owner or plan mutation', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ review: { id: 'review-1' }, replayed: false }, { status: 201 }));
    vi.stubGlobal('fetch', fetchMock);

    await api.generateWeeklyReview('2026-08-16');

    const [path, request] = fetchMock.mock.calls[1];
    expect(path).toBe('/api/v1/tracking/weekly-reviews');
    expect(JSON.parse(request?.body as string)).toEqual({ windowEnd: '2026-08-16' });
  });

  it('parses named coach SSE events in order over an authenticated POST', async () => {
    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode('event:metadata\ndata:{"conversationId":"c1","scene":"DAILY_CHECKIN"}\n\n'));
        controller.enqueue(encoder.encode('event:token\ndata:{"sequence":1,"text":"先记录"}\n\nevent:completion\ndata:{"conversationId":"c1"}\n\n'));
        controller.close();
      },
    });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(new Response(stream, { status: 200, headers: { 'Content-Type': 'text/event-stream' } }));
    vi.stubGlobal('fetch', fetchMock);
    const events: unknown[] = [];

    await api.streamCoach({ conversationId: 'c1', scene: 'DAILY_CHECKIN', message: '今天怎么开始？' }, {
      onEvent: (event) => events.push(event),
    });

    expect(events).toEqual([
      { type: 'metadata', conversationId: 'c1', scene: 'DAILY_CHECKIN' },
      { type: 'token', sequence: 1, text: '先记录' },
      { type: 'completion', conversationId: 'c1' },
    ]);
    const [, request] = fetchMock.mock.calls[1];
    expect(request?.method).toBe('POST');
    expect(request?.credentials).toBe('include');
    expect(JSON.parse(request?.body as string)).toEqual({
      conversationId: 'c1', scene: 'DAILY_CHECKIN', message: '今天怎么开始？',
    });
  });

  it('preserves a typed retryable terminal coach error', async () => {
    const encoder = new TextEncoder();
    const stream = new ReadableStream({ start(controller) {
      controller.enqueue(encoder.encode('event:metadata\ndata:{"conversationId":"c1","scene":"GENERAL_CHAT"}\n\n'));
      controller.enqueue(encoder.encode('event:error\ndata:{"code":"MODEL_TIMEOUT","message":"Timed out","retryable":true}\n\n'));
      controller.close();
    } });
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(new Response(stream, { headers: { 'Content-Type': 'text/event-stream' } })));
    const events: unknown[] = [];

    await api.streamCoach({ conversationId: 'c1', scene: 'GENERAL_CHAT', message: 'Hello' }, {
      onEvent: (event) => events.push(event),
    });

    expect(events.at(-1)).toEqual({ type: 'error', code: 'MODEL_TIMEOUT', message: 'Timed out', retryable: true });
  });
});
