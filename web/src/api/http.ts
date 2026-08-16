import type {
  HbtiDefinition,
  HbtiResult,
  Profile,
  ProfileInput,
  SafetyScreening,
  ScreeningInput,
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
};

export function isApiError(error: unknown, code?: string): error is ApiError {
  return error instanceof ApiError && (code === undefined || error.code === code);
}

export function resetCsrfTokenForTests() {
  csrfPromise = undefined;
}
