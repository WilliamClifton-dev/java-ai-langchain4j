# ADR-003: Use Cookie-Delivered JWT And Rotating Refresh Tokens

## Status

Accepted

## Date

2026-08-14

## Context

The public beta needs same-origin browser authentication, optional API bearer access, revocable sessions, and server-derived ownership without adding a separate identity service. Health-adjacent user data makes raw token storage, browser JavaScript token access, and indefinite sessions unacceptable.

The backend is one Spring Boot modular monolith. MySQL is already the durable system of record, while Redis is not available until the later distributed-control task.

## Decision

- Issue 15-minute HS256 access JWTs containing only issuer, subject, token type, ID, and lifecycle claims.
- Deliver browser access and refresh values in `HttpOnly`, `SameSite=Strict` cookies; require `Secure` outside explicitly configured local/test HTTP.
- Accept an RFC 6750 Authorization bearer token for API clients. Reject requests that present conflicting header and Cookie tokens.
- Keep Spring Security CSRF protection for Cookie-authenticated state-changing requests and expose a CSRF bootstrap endpoint.
- Issue 256-bit opaque refresh values, persist only SHA-256 digests, rotate on each use under a row lock, and revoke the full family when a replaced token is replayed.
- Use a bounded local login-attempt guard at L1 single-instance scale. Redis becomes the shared rate-limit authority before multiple instances are enabled.
- Require the signing key from deployment configuration and reject keys shorter than 32 UTF-8 bytes.

## Alternatives Considered

### Server-Side Sessions

Spring Session with Redis provides immediate revocation and simple browser semantics, but Redis is not yet part of the verified runtime and would put authentication availability on an unfinished dependency. It remains a viable future migration if operational evidence favors opaque sessions.

### Tokens In Browser Local Storage

This simplifies header construction but exposes bearer material to injected JavaScript. It was rejected because HttpOnly cookies reduce that theft surface.

### Disable CSRF Because JWT Is Stateless

Statelessness does not prevent browsers from automatically attaching cookies. Disabling CSRF would leave Cookie-authenticated writes exposed, so CSRF remains enabled.

### Asymmetric JWT Signing

RSA or EC keys are preferable when multiple independent services validate tokens. The L1 monolith issues and validates its own tokens, so a 256-bit-or-greater HMAC secret is operationally simpler. Service decomposition or external validation requires a new ADR and a multi-key rotation plan.

## Consequences

- A database compromise does not reveal reusable refresh values, but active Cookie theft still requires HTTPS, browser hardening, and short access TTLs.
- Reuse detection can invalidate all descendants of a stolen token family.
- Signing-key rotation is not yet zero-downtime because one key is configured. L1 release work must either add a verification key ring or document a coordinated session-invalidating rotation.
- The per-process guard is intentionally conservative and bounded, but not globally consistent. Task 16 must add Redis enforcement before horizontal scaling.
- Browser clients must obtain and send the CSRF token as well as allowing credentials on same-origin requests.
