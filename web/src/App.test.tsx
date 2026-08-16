import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from './App';
import { resetCsrfTokenForTests } from './api/http';

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const session = {
  user: { id: 'account-123', email: 'user@example.com' },
  accessExpiresAt: '2026-08-16T14:00:00Z',
};

describe('account experience', () => {
  afterEach(() => {
    resetCsrfTokenForTests();
    vi.unstubAllGlobals();
  });

  it('recovers an expired access session through the refresh cookie', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'UNAUTHENTICATED', message: 'Authentication required', details: {} } }, 401))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse(session))
      .mockResolvedValueOnce(jsonResponse(session));
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(await screen.findByRole('heading', { name: '身份信息' })).toBeInTheDocument();
    expect(screen.getAllByText('user@example.com')).toHaveLength(2);
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/api/v1/auth/session',
      '/api/v1/auth/csrf',
      '/api/v1/auth/refresh',
      '/api/v1/auth/session',
    ]);
  });

  it('lets an anonymous user log in without storing a browser token', async () => {
    window.history.replaceState({}, '', '/?view=account#identity');
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'UNAUTHENTICATED', message: 'Authentication required', details: {} } }, 401))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'INVALID_REFRESH_TOKEN', message: 'Refresh token invalid', details: {} } }, 401))
      .mockResolvedValueOnce(jsonResponse(session));
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);

    await user.type(await screen.findByLabelText('邮箱'), 'user@example.com');
    await user.type(screen.getByLabelText('密码'), 'correct horse battery staple');
    await user.click(screen.getByRole('button', { name: '登录' }));

    expect(await screen.findByRole('heading', { name: '身份信息' })).toBeInTheDocument();
    const loginRequest = fetchMock.mock.calls[3];
    expect(loginRequest[0]).toBe('/api/v1/auth/login');
    expect(JSON.parse(loginRequest[1]?.body as string)).toEqual({
      email: 'user@example.com',
      password: 'correct horse battery staple',
    });
    expect(window.localStorage).toHaveLength(0);
    expect(`${window.location.search}${window.location.hash}`).toBe('?view=account#identity');
  });

  it('clears the local session after an idempotent logout', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(session))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: '退出登录' }));

    await waitFor(() => expect(screen.getByRole('heading', { name: '登录你的工作区' })).toBeInTheDocument());
    expect(fetchMock.mock.calls[2][0]).toBe('/api/v1/auth/logout');
  });

  it('keeps the session visible when server-side logout is not confirmed', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(session))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockRejectedValueOnce(new TypeError('network unavailable'));
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: '退出登录' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('当前会话仍然有效');
    expect(screen.getByRole('heading', { name: '身份信息' })).toBeInTheDocument();
  });

  it('restores the persisted profile and safety gate', async () => {
    window.history.replaceState({}, '', '/profile');
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(session))
      .mockResolvedValueOnce(jsonResponse({
        userId: 'account-123', dateOfBirth: '1990-01-01', calculationSex: 'FEMALE',
        heightCm: 165, currentWeightKg: 70, targetWeightKg: 60,
        activityLevel: 'MODERATE', timeZone: 'Asia/Hong_Kong',
      }))
      .mockResolvedValueOnce(jsonResponse({
        id: 'screen-1', version: 1, status: 'ELIGIBLE', automaticPlanningAllowed: true,
        reasonCodes: [], guidance: 'Automatic planning is available.', createdAt: '2026-08-16T12:00:00Z',
      }));
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(await screen.findByRole('heading', { name: '个人档案与安全筛查' })).toBeInTheDocument();
    expect(await screen.findByText('可以进入自动计划')).toBeInTheDocument();
    expect(screen.getByDisplayValue('70')).toBeInTheDocument();
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/api/v1/auth/session', '/api/v1/profile', '/api/v1/profile/screenings/current',
    ]);
  });
});
