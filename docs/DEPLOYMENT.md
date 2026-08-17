# Deployment Guide

## Delivery Boundary

The repository ships two independently built application images and two runtime
dependencies:

| Service | Role | Host exposure |
|---|---|---|
| `web` | Nginx static SPA plus same-origin `/api` and `/actuator` proxy | `5173` by default |
| `backend` | Spring Boot REST/SSE application | `8080` by default for diagnostics |
| `mysql` | Durable source of truth and Flyway migrations | internal network only |
| `redis` | Bounded ephemeral controls and reconstructable cache | internal network only |

The included Compose file is a production-like L1 verification environment, not a
complete cloud production topology. It does not provide TLS termination, managed
secrets, off-host backups, alert delivery, or multi-instance orchestration.

## Prerequisites

- Docker Engine with Docker Compose v2
- PowerShell 7 for the asserting smoke script
- JDK 17, Maven, and Node.js 22 only for host-side development

## Configuration

`.env.example` is the complete variable catalog. Copy it to an ignored `.env` only
when values need to differ from the safe local Compose defaults. Never place real
credentials in source, image build arguments, or command history.

Compose defaults to `APP_PROFILE=offline`. In that profile the account, profile,
assessment, deterministic plan, tracking, review, export and deletion capabilities
work normally and model calls never contact a provider. The Web streaming coach
returns the existing typed model-unavailable event; the legacy synchronous coach
endpoint is not part of the offline public-beta entry path.

To enable a provider, explicitly set one of:

```text
APP_PROFILE=minimax + MINIMAX_API_KEY
APP_PROFILE=local   + OLLAMA_BASE_URL + OLLAMA_MODEL_NAME
```

Production must also replace `MYSQL_PASSWORD` and `AUTH_SIGNING_KEY`, set
`AUTH_SECURE_COOKIES=true`, set the exact HTTPS `CORS_ALLOWED_ORIGINS`, and terminate
TLS before browser traffic reaches the Web service. The local defaults are not
production credentials.

## Compose Start And Smoke

Start the four services and retain them:

```powershell
docker compose up --build --detach --wait
docker compose ps
```

Run the release-style smoke test, which builds from clean source, waits on health,
asserts all four services, checks direct backend readiness, checks readiness through
the Web proxy, verifies both application containers run as non-root, verifies the SPA
shell, saves evidence under `target/compose-smoke/`, and cleans up its isolated project:

```powershell
./scripts/smoke/compose-smoke.ps1
```

Use `-KeepRunning` only for interactive inspection. Clean up that named smoke project
afterward:

```powershell
docker compose --project-name hbti-smoke down --volumes --remove-orphans
```

The user-facing application is `http://localhost:5173/`. The direct backend port is
kept for diagnostics and API clients; browser traffic should use the Web origin so
Cookie, CSRF and SSE requests remain same-origin.

## Health And Diagnostics

- Web process health: `http://localhost:5173/healthz`
- Proxied readiness: `http://localhost:5173/actuator/health/readiness`
- Direct liveness: `http://localhost:8080/actuator/health/liveness`
- Direct readiness: `http://localhost:8080/actuator/health/readiness`
- API docs: `http://localhost:8080/doc.html`

Readiness includes MySQL and Redis. Component names and aggregate status are public
for orchestration; diagnostic details require `ACTUATOR_ADMIN`. Compose configuration
expansion or a fixed sleep is not runtime health evidence.

Application logs are newline-delimited JSON. They intentionally omit request bodies,
cookies, credentials, user facts, prompts and model content. On smoke failure, inspect
`target/compose-smoke/failure.txt`, `compose-ps.txt` and `compose-logs.txt`.

## Manual Backend Build

The Spring Boot Maven plugin produces an executable archive:

```powershell
mvn clean test
mvn -DskipTests package
java -jar target/hbti-coach-1.0-SNAPSHOT.jar
```

Manual startup requires reachable MySQL and Redis plus the variables cataloged in
`.env.example`. Flyway migrations run during startup and remain append-only.

## CI Gates

`.github/workflows/ci.yml` enforces:

1. backend clean tests, executable packaging and OSV runtime audit;
2. frontend `npm ci`, tests, production build and high-severity npm audit;
3. independent backend/Web image builds and the same asserting Compose smoke script.

The package, frontend bundle, dependency report and Compose evidence are uploaded as
short-lived workflow artifacts.

## Production Checklist

- [ ] Use managed secret injection; no defaults or `.env` files in deployed artifacts.
- [ ] Use a dedicated least-privilege MySQL account and off-host encrypted backups.
- [ ] Keep Redis non-authoritative; loss of Redis must not lose user facts.
- [ ] Terminate TLS and keep `AUTH_SECURE_COOKIES=true`.
- [ ] Restrict the browser origin and direct backend exposure.
- [ ] Publish immutable image digests and the Flyway migration range.
- [ ] Configure log/metric collection and actionable alerts.
- [ ] Complete Task 23 load, AI evaluation, restore and rollback exercises.
- [ ] Record the release checklist and rollback command before public traffic.

Passing this guide's smoke test is L1 delivery evidence. It is not L2/enterprise or
30-day production-SLO evidence.
