import type {
  CoachScene,
  CoachStreamEvent,
  CoachStreamInput,
  DailyMetric,
  DailyMetricInput,
  DailySummary,
  HbtiDefinition,
  HbtiResult,
  NutritionInput,
  NutritionLog,
  Profile,
  ProfileInput,
  SafetyScreening,
  ScreeningInput,
  TrackingWrite,
  TrainingInput,
  TrainingLog,
  WeeklyReview,
  WeeklyReviewWrite,
  WeightPlan,
} from './domain';

export interface AuthUser {
  id: string;
  email: string;
}

export interface AuthSession {
  user: AuthUser;
  accessExpiresAt: string;
}

export interface AuthCredentials {
  email: string;
  password: string;
}

interface ApiErrorEnvelope {
  error?: {
    code?: string;
    message?: string;
    details?: Record<string, string>;
  };
}

interface CsrfContract {
  headerName: string;
  token: string;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

let csrfPromise: Promise<CsrfContract> | undefined;

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined;
  }

  const contentType = response.headers.get('Content-Type') ?? '';
  if (!contentType.includes('application/json')) {
    return undefined;
  }

  return response.json();
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(path, {
    ...init,
    headers,
    credentials: 'include',
  });
  const body = await parseBody(response);

  if (!response.ok) {
    const envelope = (body ?? {}) as ApiErrorEnvelope;
    throw new ApiError(
      response.status,
      envelope.error?.code ?? 'REQUEST_FAILED',
      envelope.error?.message ?? '请求未能完成，请稍后重试',
      envelope.error?.details ?? {},
    );
  }

  return body as T;
}

function csrfContract(): Promise<CsrfContract> {
  csrfPromise ??= request<CsrfContract>('/api/v1/auth/csrf').catch((error: unknown) => {
    csrfPromise = undefined;
    throw error;
  });
  return csrfPromise;
}

async function mutate<T>(path: string, init: RequestInit = {}): Promise<T> {
  const csrf = await csrfContract();
  const headers = new Headers(init.headers);
  headers.set(csrf.headerName, csrf.token);
  return request<T>(path, { ...init, method: init.method ?? 'POST', headers });
}

const COACH_SCENES = new Set<CoachScene>([
  'GENERAL_CHAT', 'PLAN_GENERATION', 'DAILY_CHECKIN', 'WEEKLY_REVIEW',
  'HBTI_INTERPRETATION', 'SAFETY_SCREENING',
]);

function objectValue(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练返回了无法识别的数据');
  }
  return value as Record<string, unknown>;
}

function stringValue(value: unknown) {
  if (typeof value !== 'string') throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练返回了无法识别的数据');
  return value;
}

function coachEvent(name: string, json: string): CoachStreamEvent {
  let value: Record<string, unknown>;
  try {
    value = objectValue(JSON.parse(json));
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练返回了无法识别的数据');
  }

  if (name === 'metadata') {
    const scene = stringValue(value.scene) as CoachScene;
    if (!COACH_SCENES.has(scene)) throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练场景无效');
    return { type: 'metadata', conversationId: stringValue(value.conversationId), scene };
  }
  if (name === 'token') {
    if (!Number.isInteger(value.sequence) || (value.sequence as number) < 1) {
      throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练消息顺序无效');
    }
    return { type: 'token', sequence: value.sequence as number, text: stringValue(value.text) };
  }
  if (name === 'completion') {
    return { type: 'completion', conversationId: stringValue(value.conversationId) };
  }
  if (name === 'error') {
    if (typeof value.retryable !== 'boolean') throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练错误状态无效');
    return {
      type: 'error', code: stringValue(value.code), message: stringValue(value.message),
      retryable: value.retryable,
    };
  }
  throw new ApiError(502, 'INVALID_STREAM_EVENT', '教练返回了未知事件');
}

async function streamCoach(
  input: CoachStreamInput,
  handlers: { onEvent(event: CoachStreamEvent): void },
  signal?: AbortSignal,
) {
  const csrf = await csrfContract();
  const response = await fetch('/api/v1/coach/messages/stream', {
    method: 'POST',
    credentials: 'include',
    signal,
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(input),
  });

  if (!response.ok) {
    const body = await parseBody(response) as ApiErrorEnvelope | undefined;
    throw new ApiError(
      response.status,
      body?.error?.code ?? 'REQUEST_FAILED',
      body?.error?.message ?? '暂时无法连接教练',
      body?.error?.details ?? {},
    );
  }
  if (!response.headers.get('Content-Type')?.includes('text/event-stream') || !response.body) {
    throw new ApiError(502, 'INVALID_STREAM_RESPONSE', '教练流式响应不可用');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let hasMetadata = false;
  let terminal = false;
  let lastSequence = 0;

  function dispatch(block: string) {
    const lines = block.split('\n');
    const name = lines.find((line) => line.startsWith('event:'))?.slice(6).trim();
    const data = lines.filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trimStart()).join('\n');
    if (!name || !data) return;
    const event = coachEvent(name, data);
    if (terminal || (event.type === 'metadata' && hasMetadata) || (event.type !== 'metadata' && !hasMetadata)) {
      throw new ApiError(502, 'INVALID_STREAM_ORDER', '教练消息事件顺序无效');
    }
    if (event.type === 'metadata') hasMetadata = true;
    if (event.type === 'token') {
      if (event.sequence !== lastSequence + 1) throw new ApiError(502, 'INVALID_STREAM_ORDER', '教练消息片段顺序无效');
      lastSequence = event.sequence;
    }
    if (event.type === 'completion' || event.type === 'error') terminal = true;
    handlers.onEvent(event);
  }

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value, { stream: !done }).replaceAll('\r\n', '\n');
    let boundary = buffer.indexOf('\n\n');
    while (boundary >= 0) {
      dispatch(buffer.slice(0, boundary));
      buffer = buffer.slice(boundary + 2);
      boundary = buffer.indexOf('\n\n');
    }
    if (done) break;
  }
  if (!terminal) throw new ApiError(502, 'STREAM_INTERRUPTED', '教练连接在完成前中断');
}

export const api = {
  getSession: () => request<AuthSession>('/api/v1/auth/session'),
  register: (credentials: AuthCredentials) => mutate<AuthSession>('/api/v1/auth/register', {
    body: JSON.stringify(credentials),
  }),
  login: (credentials: AuthCredentials) => mutate<AuthSession>('/api/v1/auth/login', {
    body: JSON.stringify(credentials),
  }),
  refresh: () => mutate<AuthSession>('/api/v1/auth/refresh'),
  logout: () => mutate<void>('/api/v1/auth/logout'),
  getProfile: () => request<Profile>('/api/v1/profile'),
  saveProfile: (profile: ProfileInput) => mutate<Profile>('/api/v1/profile', {
    method: 'PUT',
    body: JSON.stringify(profile),
  }),
  getCurrentScreening: () => request<SafetyScreening>('/api/v1/profile/screenings/current'),
  createScreening: (answers: ScreeningInput) => mutate<SafetyScreening>('/api/v1/profile/screenings', {
    body: JSON.stringify(answers),
  }),
  getHbtiDefinition: (version: string) => request<HbtiDefinition>(
    `/api/v1/assessments/hbti/definitions/${encodeURIComponent(version)}`,
  ),
  getCurrentHbtiResult: () => request<HbtiResult>('/api/v1/assessments/hbti/results/current'),
  submitHbti: (definitionVersion: string, answers: Array<{ itemKey: string; value: number }>, idempotencyKey: string) =>
    mutate<{ result: HbtiResult; replayed: boolean }>('/api/v1/assessments/hbti/submissions', {
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({ definitionVersion, answers }),
    }),
  getActivePlan: () => request<WeightPlan>('/api/v1/plans/active'),
  createPlanDraft: (goal: WeightPlan['goal'], idempotencyKey: string) => mutate<WeightPlan>('/api/v1/plans/drafts', {
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ goal }),
  }),
  transitionPlan: (plan: Pick<WeightPlan, 'planId' | 'id'>, action: 'validation' | 'confirmation') =>
    mutate<WeightPlan>(`/api/v1/plans/${encodeURIComponent(plan.planId)}/versions/${encodeURIComponent(plan.id)}/${action}`),
  activatePlan: (plan: Pick<WeightPlan, 'planId' | 'id'>, idempotencyKey: string) =>
    mutate<WeightPlan>(`/api/v1/plans/${encodeURIComponent(plan.planId)}/versions/${encodeURIComponent(plan.id)}/activation`, {
      headers: { 'Idempotency-Key': idempotencyKey },
    }),
  getDailySummary: (localDate: string) => request<DailySummary>(
    `/api/v1/tracking/days/${encodeURIComponent(localDate)}`,
  ),
  recordDailyMetric: (input: DailyMetricInput, idempotencyKey: string) =>
    mutate<TrackingWrite<DailyMetric>>('/api/v1/tracking/daily-metrics', {
      headers: { 'Idempotency-Key': idempotencyKey }, body: JSON.stringify(input),
    }),
  recordNutrition: (input: NutritionInput, idempotencyKey: string) =>
    mutate<TrackingWrite<NutritionLog>>('/api/v1/tracking/nutrition', {
      headers: { 'Idempotency-Key': idempotencyKey }, body: JSON.stringify(input),
    }),
  recordTraining: (input: TrainingInput, idempotencyKey: string) =>
    mutate<TrackingWrite<TrainingLog>>('/api/v1/tracking/training', {
      headers: { 'Idempotency-Key': idempotencyKey }, body: JSON.stringify(input),
    }),
  generateWeeklyReview: (windowEnd: string) => mutate<WeeklyReviewWrite>(
    '/api/v1/tracking/weekly-reviews', { body: JSON.stringify({ windowEnd }) },
  ),
  getWeeklyReview: (reviewId: string) => request<WeeklyReview>(
    `/api/v1/tracking/weekly-reviews/${encodeURIComponent(reviewId)}`,
  ),
  streamCoach,
};

export function isApiError(error: unknown, code?: string): error is ApiError {
  return error instanceof ApiError && (code === undefined || error.code === code);
}

export function resetCsrfTokenForTests() {
  csrfPromise = undefined;
}
