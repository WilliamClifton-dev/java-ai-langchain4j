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

  it('walks through the HBTI questionnaire and submits all answers once', async () => {
    window.history.replaceState({}, '', '/assessment');
    const items = Array.from({ length: 16 }, (_, index) => ({
      itemKey: `q${index + 1}`, ordinal: index + 1, titleZh: `问题 ${index + 1}`,
      hintZh: '请选择最符合你近况的选项', titleEn: `Question ${index + 1}`, hintEn: 'Choose one',
    }));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(session))
      .mockResolvedValueOnce(jsonResponse({
        version: '1.0.0', displayName: 'HBTI', answerMin: 1, answerMax: 5,
        dimensions: [], items, limitation: 'HBTI is an exploratory behavioral tendency assessment, not a diagnosis.',
      }))
      .mockResolvedValueOnce(jsonResponse({ error: { code: 'ASSESSMENT_RESULT_NOT_FOUND', message: 'No result', details: {} } }, 404))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({
        result: { id: 'r1', definitionVersion: '1.0.0', scoringRuleVersion: '1.0.0', dimensions: [], typeCode: 'FHRN', limitation: 'HBTI is an exploratory behavioral tendency assessment, not a diagnosis.', completedAt: '2026-08-16T12:00:00Z' }, replayed: false,
      }));
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'HBTI 行为倾向测评' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '开始测评' }));
    for (let index = 0; index < 16; index += 1) {
      await user.click(screen.getByRole('radio', { name: '3' }));
      await user.click(screen.getByRole('button', { name: index === 15 ? '提交测评' : '下一题' }));
    }
    expect(await screen.findByRole('heading', { name: 'HBTI 维度画像' })).toBeInTheDocument();
    expect(screen.getByText('FHRN')).toBeInTheDocument();
    expect(fetchMock.mock.calls[4][0]).toBe('/api/v1/assessments/hbti/submissions');
    expect(new Headers(fetchMock.mock.calls[4][1]?.headers).get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(fetchMock.mock.calls[4][1]?.body as string).answers).toHaveLength(16);
  });

  it('creates, validates, confirms and activates a server-calculated plan', async () => {
    window.history.replaceState({}, '', '/plan');
    const plan = (status: string) => ({
      id: 'version-1', planId: 'plan-1', versionNo: 1, status, goal: 'LOSS',
      formulaVersion: 'MIFFLIN_ST_JEOR_METRIC_V1', targetPolicyVersion: 'BOUNDED_TARGET_POLICY_V1',
      bmi: 25.7, bmrKcalPerDay: 1450, tdeeKcalPerDay: 2100,
      energyMinKcalPerDay: 1750, energyMaxKcalPerDay: 1900,
      weeklyWeightChangeMinPercent: -0.75, weeklyWeightChangeMaxPercent: -0.25,
      createdAt: '2026-08-16T12:00:00Z', validatedAt: status === 'DRAFT' ? null : '2026-08-16T12:01:00Z',
      confirmedAt: ['CONFIRMED', 'ACTIVE'].includes(status) ? '2026-08-16T12:02:00Z' : null,
      activatedAt: status === 'ACTIVE' ? '2026-08-16T12:03:00Z' : null, replacedAt: null,
      guidance: 'Targets are planning estimates, not medical prescriptions or guaranteed outcomes.',
    });
    const calls: string[] = [];
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input);
      calls.push(`${init?.method ?? 'GET'} ${path}`);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/profile') return Promise.resolve(jsonResponse({ userId: 'account-123', dateOfBirth: '1990-01-01', calculationSex: 'FEMALE', heightCm: 165, currentWeightKg: 70, targetWeightKg: 60, activityLevel: 'MODERATE', timeZone: 'Asia/Hong_Kong' }));
      if (path === '/api/v1/profile/screenings/current') return Promise.resolve(jsonResponse({ id: 'screen-1', version: 1, status: 'ELIGIBLE', automaticPlanningAllowed: true, reasonCodes: [], guidance: 'Automatic planning is available.', createdAt: '2026-08-16T12:00:00Z' }));
      if (path === '/api/v1/assessments/hbti/results/current') return Promise.resolve(jsonResponse({ id: 'result-1', definitionVersion: '1.0.0', scoringRuleVersion: '1.0.0', dimensions: [], typeCode: 'FHRN', limitation: 'HBTI is exploratory.', completedAt: '2026-08-16T12:00:00Z' }));
      if (path === '/api/v1/plans/active') return Promise.resolve(jsonResponse({ error: { code: 'PLAN_VERSION_NOT_FOUND', message: 'No active plan', details: {} } }, 404));
      if (path === '/api/v1/auth/csrf') return Promise.resolve(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }));
      if (path === '/api/v1/plans/drafts') return Promise.resolve(jsonResponse(plan('DRAFT'), 201));
      if (path.endsWith('/validation')) return Promise.resolve(jsonResponse(plan('VALIDATED')));
      if (path.endsWith('/confirmation')) return Promise.resolve(jsonResponse(plan('CONFIRMED')));
      if (path.endsWith('/activation')) return Promise.resolve(jsonResponse(plan('ACTIVE')));
      return Promise.reject(new Error(`unexpected request ${path}`));
    }));

    const user = userEvent.setup();
    render(<App />);
    expect(await screen.findByRole('heading', { name: '体重管理计划' })).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: '温和减重' }));
    await user.click(screen.getByRole('button', { name: '生成计划草稿' }));
    expect(await screen.findByText('草稿')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '校验计划' }));
    await user.click(await screen.findByRole('button', { name: '确认计划' }));
    await user.click(await screen.findByRole('button', { name: '启用计划' }));
    expect(await screen.findByText('当前计划已启用')).toBeInTheDocument();
    expect(calls).toContain('POST /api/v1/plans/drafts');
    expect(calls).toContain('POST /api/v1/plans/plan-1/versions/version-1/validation');
    expect(calls).toContain('POST /api/v1/plans/plan-1/versions/version-1/confirmation');
    expect(calls).toContain('POST /api/v1/plans/plan-1/versions/version-1/activation');
    const fetchCalls = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls;
    const draftRequest = fetchCalls.find((call) => call[0] === '/api/v1/plans/drafts');
    const activationRequest = fetchCalls.find((call) => String(call[0]).endsWith('/activation'));
    expect(new Headers(draftRequest?.[1]?.headers).get('Idempotency-Key')).toBeTruthy();
    expect(new Headers(activationRequest?.[1]?.headers).get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(draftRequest?.[1]?.body as string)).toEqual({ goal: 'LOSS' });
  });

  it('blocks planning when the persisted safety gate requires professional review', async () => {
    window.history.replaceState({}, '', '/plan');
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/profile') return Promise.resolve(jsonResponse({ userId: 'account-123', dateOfBirth: '1990-01-01', calculationSex: 'FEMALE', heightCm: 165, currentWeightKg: 70, targetWeightKg: 60, activityLevel: 'MODERATE', timeZone: 'Asia/Hong_Kong' }));
      if (path === '/api/v1/profile/screenings/current') return Promise.resolve(jsonResponse({ id: 'screen-1', version: 1, status: 'PROFESSIONAL_REVIEW', automaticPlanningAllowed: false, reasonCodes: ['MEDICAL_GUIDANCE_REQUIRED'], guidance: 'Please consult a qualified professional before planning.', createdAt: '2026-08-16T12:00:00Z' }));
      if (path === '/api/v1/assessments/hbti/results/current') return Promise.resolve(jsonResponse({ id: 'result-1', definitionVersion: '1.0.0', scoringRuleVersion: '1.0.0', dimensions: [], typeCode: 'FHRN', limitation: 'HBTI is exploratory.', completedAt: '2026-08-16T12:00:00Z' }));
      if (path === '/api/v1/plans/active') return Promise.resolve(jsonResponse({ error: { code: 'PLAN_VERSION_NOT_FOUND', message: 'No active plan', details: {} } }, 404));
      return Promise.reject(new Error(`unexpected request ${path}`));
    }));

    render(<App />);
    expect(await screen.findByText('自动计划已暂停')).toBeInTheDocument();
    expect(screen.getByText('请先完成安全筛查并确认可以进入自动计划。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '生成计划草稿' })).not.toBeInTheDocument();
  });

  it('keeps an existing active plan visible when new planning becomes blocked', async () => {
    window.history.replaceState({}, '', '/plan');
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/profile') return Promise.resolve(jsonResponse({ userId: 'account-123', dateOfBirth: '1990-01-01', calculationSex: 'FEMALE', heightCm: 165, currentWeightKg: 70, targetWeightKg: 60, activityLevel: 'MODERATE', timeZone: 'Asia/Hong_Kong' }));
      if (path === '/api/v1/profile/screenings/current') return Promise.resolve(jsonResponse({ id: 'screen-2', version: 2, status: 'PROFESSIONAL_REVIEW', automaticPlanningAllowed: false, reasonCodes: ['MEDICAL_GUIDANCE_REQUIRED'], guidance: 'Professional review required.', createdAt: '2026-08-16T13:00:00Z' }));
      if (path === '/api/v1/assessments/hbti/results/current') return Promise.resolve(jsonResponse({ id: 'result-1', definitionVersion: '1.0.0', scoringRuleVersion: '1.0.0', dimensions: [], typeCode: 'FHRN', limitation: 'HBTI is exploratory.', completedAt: '2026-08-16T12:00:00Z' }));
      if (path === '/api/v1/plans/active') return Promise.resolve(jsonResponse({ id: 'version-1', planId: 'plan-1', versionNo: 1, status: 'ACTIVE', goal: 'LOSS', formulaVersion: 'MIFFLIN_ST_JEOR_METRIC_V1', targetPolicyVersion: 'BOUNDED_TARGET_POLICY_V1', bmi: 25.7, bmrKcalPerDay: 1450, tdeeKcalPerDay: 2100, energyMinKcalPerDay: 1750, energyMaxKcalPerDay: 1900, weeklyWeightChangeMinPercent: -0.75, weeklyWeightChangeMaxPercent: -0.25, createdAt: '2026-08-16T12:00:00Z', validatedAt: '2026-08-16T12:01:00Z', confirmedAt: '2026-08-16T12:02:00Z', activatedAt: '2026-08-16T12:03:00Z', replacedAt: null, guidance: 'Planning estimate only.' }));
      return Promise.reject(new Error(`unexpected request ${path}`));
    }));

    render(<App />);
    expect(await screen.findByText('当前计划已启用')).toBeInTheDocument();
    expect(screen.getByText('已启用')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '生成计划草稿' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '制定新计划' })).not.toBeInTheDocument();
  });

  it('lets an eligible user start a replacement from an active plan', async () => {
    window.history.replaceState({}, '', '/plan');
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/profile') return Promise.resolve(jsonResponse({ userId: 'account-123' }));
      if (path === '/api/v1/profile/screenings/current') return Promise.resolve(jsonResponse({ status: 'ELIGIBLE', automaticPlanningAllowed: true, guidance: 'Eligible.' }));
      if (path === '/api/v1/assessments/hbti/results/current') return Promise.resolve(jsonResponse({ typeCode: 'FHRN' }));
      if (path === '/api/v1/plans/active') return Promise.resolve(jsonResponse({ id: 'version-1', planId: 'plan-1', versionNo: 1, status: 'ACTIVE', goal: 'LOSS', bmi: 25.7, bmrKcalPerDay: 1450, tdeeKcalPerDay: 2100, energyMinKcalPerDay: 1750, energyMaxKcalPerDay: 1900, guidance: 'Planning estimate only.' }));
      return Promise.reject(new Error(`unexpected request ${path}`));
    }));

    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByRole('button', { name: '制定新计划' }));
    expect(screen.getByRole('radio', { name: '温和减重' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生成计划草稿' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '返回当前计划' }));
    expect(await screen.findByText('当前计划已启用')).toBeInTheDocument();
  });

  it('records explicit-unit daily facts and refreshes the selected-day summary', async () => {
    window.history.replaceState({}, '', '/tracking');
    let saved = false;
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/auth/csrf') return Promise.resolve(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }));
      if (path.startsWith('/api/v1/tracking/days/')) return Promise.resolve(jsonResponse({
        localDate: path.split('/').at(-1),
        metric: saved ? { id: 'metric-1', localDate: path.split('/').at(-1), weightKg: 70.2, steps: 8000, activityMinutes: 45, sleepMinutes: 450, sleepQuality: 4, createdAt: '2026-08-16T12:00:00Z' } : null,
        nutrition: null, trainingSessions: [], trainingMinutes: 0,
      }));
      if (path === '/api/v1/tracking/daily-metrics') {
        saved = true;
        return Promise.resolve(jsonResponse({ record: { id: 'metric-1' }, replayed: false }, 201));
      }
      return Promise.reject(new Error(`unexpected request ${path}`));
    });
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);
    expect(await screen.findByRole('heading', { name: '每日执行记录' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('体重（kg）'), '70.2');
    await user.type(screen.getByLabelText('步数（步）'), '8000');
    await user.click(screen.getByRole('button', { name: '保存身体与活动记录' }));

    expect(await screen.findByText('70.2 kg')).toBeInTheDocument();
    const write = fetchMock.mock.calls.find((call) => call[0] === '/api/v1/tracking/daily-metrics');
    expect(new Headers(write?.[1]?.headers).get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(write?.[1]?.body as string)).toMatchObject({ weightKg: 70.2, steps: 8000 });
  });

  it('renders a sparse weekly review as a proposal that does not change the plan', async () => {
    window.history.replaceState({}, '', '/review');
    const fetchMock = vi.fn((input: RequestInfo | URL, _init?: RequestInit) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/auth/csrf') return Promise.resolve(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }));
      if (path === '/api/v1/tracking/weekly-reviews') return Promise.resolve(jsonResponse({
        replayed: false,
        review: {
          id: 'review-1', planVersionId: 'plan-version-1', windowStart: '2026-08-10', windowEnd: '2026-08-16',
          versionNo: 1, policyVersion: 'DETERMINISTIC_WEEKLY_REVIEW_V1', weightObservationDays: 1,
          nutritionLoggedDays: 2, stepsObservedDays: 2, sleepObservedDays: 1, trainingDays: 1,
          averageWeightKg: 70.2, weightTrendPercent: null, nutritionAdherencePercent: 50,
          averageSteps: 7000, averageSleepMinutes: 420, totalTrainingMinutes: 45,
          recommendation: 'INSUFFICIENT_DATA', proposedEnergyDeltaKcalPerDay: 0,
          reason: 'More observations are required.', createdAt: '2026-08-16T12:00:00Z',
          limitation: 'Proposed changes are not applied automatically.',
        },
      }, 201));
      return Promise.reject(new Error(`unexpected request ${path}`));
    });
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);
    expect(await screen.findByRole('heading', { name: '七日回顾' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '生成七日回顾' }));

    expect(await screen.findByRole('heading', { name: '数据不足' })).toBeInTheDocument();
    expect(screen.getByText(/不会自动修改当前计划/)).toBeInTheDocument();
    const request = fetchMock.mock.calls.find((call) => call[0] === '/api/v1/tracking/weekly-reviews');
    expect(JSON.parse(request?.[1]?.body as string)).toEqual({ windowEnd: expect.any(String) });
  });

  it('streams a coach response without sending owner or tool permissions', async () => {
    window.history.replaceState({}, '', '/coach');
    const encoder = new TextEncoder();
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input);
      if (path === '/api/v1/auth/session') return Promise.resolve(jsonResponse(session));
      if (path === '/api/v1/auth/csrf') return Promise.resolve(jsonResponse({ headerName: 'X-XSRF-TOKEN', token: 'csrf-value' }));
      if (path === '/api/v1/coach/messages/stream') return Promise.resolve(new Response(new ReadableStream({
        start(controller) {
          controller.enqueue(encoder.encode('event:metadata\ndata:{"conversationId":"c1","scene":"DAILY_CHECKIN"}\n\n'));
          controller.enqueue(encoder.encode('event:token\ndata:{"sequence":1,"text":"先记录今天的完成情况。"}\n\n'));
          controller.enqueue(encoder.encode('event:completion\ndata:{"conversationId":"c1"}\n\n'));
          controller.close();
        },
      }), { status: 200, headers: { 'Content-Type': 'text/event-stream' } }));
      return Promise.reject(new Error(`unexpected request ${path}`));
    });
    vi.stubGlobal('fetch', fetchMock);

    const user = userEvent.setup();
    render(<App />);
    expect(await screen.findByRole('heading', { name: '智能教练' })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('对话场景'), 'DAILY_CHECKIN');
    await user.type(screen.getByLabelText('你的问题'), '今天应该复盘什么？');
    await user.click(screen.getByRole('button', { name: '发送消息' }));

    expect(await screen.findByText('先记录今天的完成情况。')).toBeInTheDocument();
    expect(screen.getByText('回复完成')).toBeInTheDocument();
    const streamCall = fetchMock.mock.calls.find((call) => call[0] === '/api/v1/coach/messages/stream');
    expect(JSON.parse(streamCall?.[1]?.body as string)).toEqual(expect.objectContaining({
      scene: 'DAILY_CHECKIN', message: '今天应该复盘什么？',
    }));
    expect(JSON.parse(streamCall?.[1]?.body as string)).not.toHaveProperty('owner');
    expect(JSON.parse(streamCall?.[1]?.body as string)).not.toHaveProperty('permissions');
  });
});
