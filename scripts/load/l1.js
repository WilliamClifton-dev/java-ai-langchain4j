import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://web:8080').replace(/\/$/, '');
const password = __ENV.LOAD_PASSWORD;
const runId = __ENV.RUN_ID || 'local';
const accountCount = Number(__ENV.ACCOUNT_COUNT || 20);
const concurrentSessions = Number(__ENV.CONCURRENT_SESSIONS || 20);
const concurrentDuration = __ENV.CONCURRENT_DURATION || '10s';
const concurrentSeconds = Number(__ENV.CONCURRENT_SECONDS || 10);
const sustainedRate = Number(__ENV.SUSTAINED_RPS || 20);
const sustainedDuration = __ENV.SUSTAINED_DURATION || '60s';
const burstRate = Number(__ENV.BURST_RPS || 100);
const burstDuration = __ENV.BURST_DURATION || '10s';
const sustainedSeconds = Number(__ENV.SUSTAINED_SECONDS || 60);
// Keep headroom for arrival-rate scheduling so the generator does not become
// the bottleneck when a response briefly exceeds one second.
const burstVus = Math.max(200, burstRate * 2);

const sustainedDurationMetric = new Trend('hbti_sustained_duration', true);
const burstDurationMetric = new Trend('hbti_burst_duration', true);
const interactiveDurationMetric = new Trend('hbti_interactive_duration', true);
const sustainedErrors = new Rate('hbti_sustained_errors');
const burstErrors = new Rate('hbti_burst_errors');
const interactiveErrors = new Rate('hbti_interactive_errors');
const interactiveRequests = new Counter('hbti_interactive_requests');

export const options = {
  scenarios: {
    interactive: {
      executor: 'constant-vus',
      exec: 'interactiveTraffic',
      vus: concurrentSessions,
      duration: concurrentDuration,
      gracefulStop: '5s',
    },
    sustained: {
      executor: 'constant-arrival-rate',
      exec: 'nonModelTraffic',
      startTime: `${concurrentSeconds + 5}s`,
      rate: sustainedRate,
      timeUnit: '1s',
      duration: sustainedDuration,
      preAllocatedVUs: 20,
      maxVUs: 20,
      gracefulStop: '5s',
    },
    burst: {
      executor: 'constant-arrival-rate',
      exec: 'nonModelTraffic',
      startTime: `${concurrentSeconds + sustainedSeconds + 10}s`,
      rate: burstRate,
      timeUnit: '1s',
      duration: burstDuration,
      preAllocatedVUs: burstVus,
      maxVUs: burstVus,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    hbti_sustained_duration: ['p(95)<500', 'p(99)<1500'],
    hbti_burst_duration: ['p(95)<500', 'p(99)<1500'],
    hbti_interactive_duration: ['p(95)<500', 'p(99)<1500'],
    hbti_sustained_errors: ['rate<0.01'],
    hbti_burst_errors: ['rate<0.01'],
    hbti_interactive_errors: ['rate<0.01'],
    hbti_interactive_requests: ['count>0'],
    'dropped_iterations{scenario:sustained}': ['count==0'],
    'dropped_iterations{scenario:burst}': ['count==0'],
  },
  noConnectionReuse: false,
  noCookiesReset: true,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  userAgent: 'hbti-l1-load/1.0.0',
};

function csrf(jar) {
  const response = http.get(`${baseUrl}/api/v1/auth/csrf`, {
    jar,
    tags: { name: 'setup_csrf' },
  });
  if (response.status !== 200) {
    fail(`CSRF bootstrap failed with ${response.status}`);
  }
  return response.json();
}

function mutation(method, path, body, jar, csrfContract, tags = {}) {
  return http.request(method, `${baseUrl}${path}`, JSON.stringify(body), {
    jar,
    headers: {
      'Content-Type': 'application/json',
      [csrfContract.headerName]: csrfContract.token,
    },
    tags,
  });
}

function snapshotSessionCookies(jar) {
  const cookies = jar.cookiesForURL(`${baseUrl}/api/v1/auth/session`);
  const sessionCookies = {};
  for (const [name, values] of Object.entries(cookies)) {
    const cookie = Array.isArray(values) ? values[0] : values;
    const value = typeof cookie === 'string' ? cookie : cookie.value;
    if (value) {
      sessionCookies[name] = value;
    }
  }
  return sessionCookies;
}

function restoreSessionCookies(jar, cookies) {
  for (const [name, value] of Object.entries(cookies)) {
    const cookieUrl = name === 'HBTI_ACCESS' ? baseUrl : `${baseUrl}/api/v1/auth`;
    jar.set(cookieUrl, name, value);
  }
}

export function setup() {
  if (!password || password.length < 12) {
    fail('LOAD_PASSWORD must be supplied by the runner and contain at least 12 characters.');
  }
  const accounts = [];
  const jar = http.cookieJar();
  for (let index = 0; index < accountCount; index += 1) {
    jar.clear(baseUrl);
    const email = `load-${runId}-${index}@hbti.local`;
    const token = csrf(jar);
    const registration = mutation('POST', '/api/v1/auth/register', { email, password }, jar, token, {
      name: 'setup_register',
    });
    if (registration.status !== 201) {
      fail(`Account setup failed for index ${index} with ${registration.status}`);
    }
    const profile = mutation('PUT', '/api/v1/profile', {
      dateOfBirth: '1992-04-20', calculationSex: 'FEMALE', heightCm: 165,
      currentWeightKg: 70, targetWeightKg: 64, activityLevel: 'MODERATE',
      timeZone: 'Asia/Hong_Kong',
    }, jar, token, { name: 'setup_profile' });
    if (profile.status !== 200) {
      fail(`Profile setup failed for index ${index} with ${profile.status}`);
    }
    accounts.push({ email, cookies: snapshotSessionCookies(jar) });
  }
  jar.clear(baseUrl);
  return { accounts };
}

let authenticatedEmail;

function ensureAuthenticated(data) {
  const index = (__VU - 1) % data.accounts.length;
  const account = data.accounts[index];
  const email = account.email;
  if (authenticatedEmail === email) {
    return;
  }
  const jar = http.cookieJar();
  restoreSessionCookies(jar, account.cookies);
  const session = http.get(`${baseUrl}/api/v1/auth/session`, {
    jar,
    tags: { name: 'warmup_session' },
  });
  if (session.status !== 200) {
    fail(`VU session bootstrap failed with ${session.status}`);
  }
  authenticatedEmail = email;
}

export function nonModelTraffic(data) {
  ensureAuthenticated(data);
  const jar = http.cookieJar();
  const paths = [
    '/api/v1/auth/session',
    '/api/v1/profile',
    '/api/v1/assessments/hbti/definitions/1.0.0',
  ];
  const response = http.get(`${baseUrl}${paths[__ITER % paths.length]}`, {
    jar,
    tags: { name: 'non_model_business_read' },
  });
  const ok = check(response, {
    'non-model request returns 200': (value) => value.status === 200,
  });

  if (exec.scenario.name === 'sustained') {
    sustainedDurationMetric.add(response.timings.duration);
    sustainedErrors.add(!ok);
  } else {
    burstDurationMetric.add(response.timings.duration);
    burstErrors.add(!ok);
  }
}

export function interactiveTraffic(data) {
  ensureAuthenticated(data);
  const response = http.get(`${baseUrl}/api/v1/auth/session`, {
    jar: http.cookieJar(),
    tags: { name: 'interactive_session_read' },
  });
  const ok = check(response, {
    'interactive session returns 200': (value) => value.status === 200,
  });
  interactiveDurationMetric.add(response.timings.duration);
  interactiveErrors.add(!ok);
  interactiveRequests.add(1);
  sleep(0.1);
}

export function teardown(data) {
  const jar = http.cookieJar();
  for (const account of data.accounts) {
    jar.clear(baseUrl);
    restoreSessionCookies(jar, account.cookies);
    const token = csrf(jar);
    mutation('DELETE', '/api/v1/account', { confirmation: 'DELETE_MY_ACCOUNT' }, jar, token, {
      name: 'teardown_delete',
    });
  }
}

export function handleSummary(data) {
  return {
    '/evidence/k6-summary.json': JSON.stringify(data, null, 2),
    stdout: `L1 k6 thresholds: ${Object.values(data.metrics)
      .filter((metric) => metric.thresholds)
      .every((metric) => Object.values(metric.thresholds).every((threshold) => threshold.ok)) ? 'PASS' : 'FAIL'}\n`,
  };
}
