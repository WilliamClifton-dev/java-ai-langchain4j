# ADR-016: Reject The Default Auth Signing Key Outside Development Profiles

## Status

Accepted

## Date

2026-08-21

## Context

`AuthProperties` already enforces that `hbti.auth.signing-key` contains at least
32 UTF-8 bytes, which prevents accidental short keys. The Compose stack however
falls back to a hard-coded development key
(`local-development-signing-key-min-32-bytes`) when `AUTH_SIGNING_KEY` is unset,
so an operator who forgets to inject a real secret still boots a server that
mints valid JWTs against a publicly known key. The current behaviour relies on
operator discipline and the L1 release checklist alone, which is one forgotten
environment variable away from a credential failure.

The same dev key is convenient for the offline Compose demo and the local
`local` profile that targets an Ollama runtime. Those two profiles intentionally
start without operator secrets so the deterministic features stay reviewable
without external dependencies.

## Decision

- Treat the well-known dev key as a deny-listed value. Any other value is
  accepted regardless of profile.
- The dev key is allowed only when at least one of the active profiles is in
  the allow list `{offline, local, test}`. The allow list mirrors the profile
  matrix that already runs without real model or platform secrets.
- Implement the check in a dedicated `AuthSigningKeyValidator` Spring bean with
  `@PostConstruct`. Run the check before any JWT encoder or decoder is used so a
  bad key fails fast with a clear error during context refresh instead of
  silently minting weak tokens.
- Keep `docker-compose.yml` fall-through value unchanged so the offline demo
  still starts, and surface the allow list alongside the warning so the
  platform deployment checklist remains the source of truth for production.
- Add a unit test for the validator covering the three branches (default key +
  disallowed profile throws, default key + allowed profile passes, custom key
  passes unconditionally) and a Spring Boot context test that asserts the
  default key prevents startup under the `minimax` profile.

## Alternatives Considered

### Reject the dev key unconditionally

Rejected because the offline Compose demo, `local` profile smoke and JUnit
`test` profile all depend on a deterministic signing key to exercise auth
without operator secrets. An unconditional rejection would force every dev
workstation to mint a random key on first run, which trades safety for a
small ergonomic loss that the new check already catches at deploy time.

### Move the check into the Compose file only

Rejected because the validator belongs at the application boundary. A Compose
fallback can be overridden, mirrored, or split across multiple manifests;
the application contract is the only place every deployment must honour.

### Add an opt-in environment flag to silence the validator

Rejected because opt-outs re-introduce the exact failure mode the ADR removes.
If a deployment genuinely needs to run a known dev key, that deployment is by
definition not L1 production-ready and should not be shipping.

## Consequences

- Deployments that forget `AUTH_SIGNING_KEY` under the `minimax` profile now
  refuse to start. The error message names the offending value and lists the
  permitted development profiles so the fix path is obvious.
- The offline Compose demo, the `local` Ollama profile and the JUnit `test`
  profile continue to boot without any environment change. No fixture or
  documentation outside this validator needs to move.
- The L1 release checklist already requires platform-injected secrets; this
  ADR makes that requirement enforceable at the application boundary instead
  of relying on the checklist alone.
- The deny list must grow whenever a new publicly known dev key enters the
  repository. The check is centralised so the list lives in one file.
