import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from './App';
import { resetCsrfTokenForTests } from './api/http';

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const session = (id: string) => ({
  user: { id, email: `${id}@example.com` }, accessExpiresAt: '2099-01-01T00:00:00Z',
});
const profile = (id: string, weight: number) => ({
  userId: id, dateOfBirth: '1990-01-01', calculationSex: 'FEMALE', heightCm: 165,
  currentWeightKg: weight, targetWeightKg: 60, activityLevel: 'MODERATE', timeZone: 'Asia/Hong_Kong',
});

describe('session isolation regressions', () => {
  afterEach(() => {
    cleanup();
    resetCsrfTokenForTests();
    vi.unstubAllGlobals();
  });

  it('loads and saves B own profile after A logs out in the same page', async () => {
    window.history.replaceState({}, '', '/profile');
    let account = 'account-a';
    const saved: unknown[] = [];
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return jsonResponse(session(account));
      if (path === '/api/v1/auth/csrf') return jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' });
      if (path === '/api/v1/auth/logout') return new Response(null, { status: 204 });
      if (path === '/api/v1/auth/login') {
        account = 'account-b';
        return jsonResponse(session(account));
      }
      if (path === '/api/v1/profile') {
        if (init?.method === 'PUT') {
          const body = JSON.parse(init.body as string);
          saved.push(body);
          return jsonResponse({ userId: account, ...body });
        }
        return jsonResponse(profile(account, account === 'account-a' ? 70 : 82));
      }
      if (path === '/api/v1/profile/screenings/current') return jsonResponse({
        error: { code: 'PROFILE_NOT_FOUND', message: 'No screening' },
      }, 404);
      throw new Error(`Unexpected request ${path}`);
    }));
    const user = userEvent.setup();
    render(<App />);
    expect(await screen.findByDisplayValue('70')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '退出登录' }));
    await user.type(await screen.findByLabelText('邮箱'), 'account-b@example.com');
    await user.type(screen.getByLabelText('密码'), 'correct horse battery staple');
    await user.click(screen.getByRole('button', { name: '登录' }));
    await user.click(await screen.findByRole('link', { name: '档案' }));

    expect(await screen.findByDisplayValue('82')).toBeInTheDocument();
    expect(screen.queryByDisplayValue('70')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '保存档案' }));
    await waitFor(() => expect(saved).toEqual([expect.objectContaining({ currentWeightKg: 82 })]));
  });

  it('returns to login when a protected request cannot refresh the expired session', async () => {
    let refreshCount = 0;
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return jsonResponse(session('account-a'));
      if (path === '/api/v1/auth/csrf') return jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' });
      if (path === '/api/v1/auth/refresh') refreshCount += 1;
      if (path === '/api/v1/auth/refresh' || path === '/api/v1/profile') return jsonResponse({
        error: { code: 'UNAUTHENTICATED', message: 'Session expired' },
      }, 401);
      throw new Error(`Unexpected request ${path}`);
    }));
    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByRole('link', { name: '档案' }));

    expect(await screen.findByRole('heading', { name: '登录你的工作区' })).toBeInTheDocument();
    expect(screen.queryByText('account-a@example.com')).not.toBeInTheDocument();
    expect(refreshCount).toBe(1);
  });
});
