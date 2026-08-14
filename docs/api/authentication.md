# Authentication API

Base path: `/api/v1/auth`

## Endpoints

| Method | Path | Authentication | Result |
|---|---|---|---|
| `GET` | `/csrf` | public | returns CSRF header name and token and initializes the CSRF cookie |
| `POST` | `/register` | public + CSRF | creates an account, returns user summary, and sets access/refresh cookies |
| `POST` | `/login` | public + CSRF | verifies generic credentials, returns user summary, and sets new cookies |
| `POST` | `/refresh` | refresh cookie + CSRF | rotates the refresh family member and replaces both cookies |
| `POST` | `/logout` | optional refresh cookie + CSRF | revokes the known family and expires both cookies; idempotent |

Register and login accept:

```json
{
  "email": "user@example.com",
  "password": "correct horse battery staple"
}
```

Successful session responses deliberately omit raw tokens:

```json
{
  "user": {
    "id": "8c2de830-e83b-40be-bc80-b6ad7dc68aa6",
    "email": "user@example.com"
  },
  "accessExpiresAt": "2026-08-14T08:15:00Z"
}
```

## Cookies And Headers

- `HBTI_ACCESS`: access JWT, path `/`, HttpOnly, SameSite Strict.
- `HBTI_REFRESH`: opaque refresh value, path `/api/v1/auth`, HttpOnly, SameSite Strict.
- `XSRF-TOKEN`: readable CSRF value issued by Spring Security; send it using the header named by `/csrf`.
- API clients may use `Authorization: Bearer <access-jwt>` instead of the access cookie.

Production responses set `Secure` on authentication cookies. Browser JavaScript never reads either authentication cookie.

## Stable Errors

All failures use `{ "error": { "code", "message", "details" } }`. Authentication-specific codes are:

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `INVALID_CREDENTIAL_INPUT` | credential byte or format boundary rejected |
| 401 | `INVALID_CREDENTIALS` | login failed without confirming account existence |
| 401 | `INVALID_REFRESH_TOKEN` | refresh is missing, expired, revoked, or unknown |
| 401 | `SESSION_REVOKED` | replay of a replaced refresh token revoked its family |
| 401 | `UNAUTHENTICATED` | protected route has no valid access token |
| 403 | `FORBIDDEN` | authenticated request lacks permission or CSRF proof |
| 409 | `EMAIL_ALREADY_REGISTERED` | normalized account identity already exists |
| 429 | `LOGIN_RATE_LIMITED` | bounded login failure threshold reached |

## Configuration

| Environment variable | Required/default | Purpose |
|---|---|---|
| `AUTH_SIGNING_KEY` | required, at least 32 UTF-8 bytes | HS256 signing and verification key |
| `AUTH_SECURE_COOKIES` | default `true` | may be `false` only for local HTTP or tests |

Token TTLs and issuer are typed application properties. Test configuration uses a non-secret test-only key and does not contact an external identity service.
