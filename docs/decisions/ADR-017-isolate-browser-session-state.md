# ADR-017: Isolate browser session state

Status: Accepted
Date: 2026-09-08

React Query caches previously survived logout and used the same keys for every
account. Protected API calls also failed after the access cookie expired, even
when the refresh cookie was still valid.

Each authenticated account now owns a separate QueryClient subtree. Changing the
account or logging out unmounts the previous subtree and clears its queries, so
late results cannot populate the next account's cache or forms.

The HTTP client retries a protected request once after a 401, sharing one refresh
promise across concurrent requests. Authentication endpoints do not recursively
refresh. A refresh version lets late 401 responses reuse a completed rotation;
an identity generation prevents replaying an older account's request after a
session change. Retries preserve the original body and idempotency key. SSE is
retried only after an HTTP 401 before streaming starts, never after output or a
terminal model error. Invalid refresh credentials clear the visible session;
network failures preserve it so the user can retry.

Spring Security can clear the CSRF cookie after authentication. CSRF rejections now
use the specific `INVALID_CSRF_TOKEN` code (HTTP 403); the client obtains a fresh
contract and retries that rejected request once. Ordinary authorization failures
remain `FORBIDDEN` and are never retried as CSRF failures.

Tests cover account switching and profile writes, concurrent refresh, bounded
retry, session expiry, and early SSE rejection. These changes retain cookie and
CSRF authentication and add no browser token storage or third-party dependency.
