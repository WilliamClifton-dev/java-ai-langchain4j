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
});
