# Release And Rollback Runbook

## Release

1. Use a clean commit and immutable backend/Web image digests. Record Flyway versions,
   Prompt bundle hash, HBTI definition, model policy and dependency reports.
2. Run `scripts/release/verify-release.ps1`. Model-enabled deployment additionally
   requires reviewed provider evidence for every mandatory AI case and cost boundary.
3. Back up MySQL, verify the checksum and confirm a successful restore drill within the
   release evidence window. Flyway migrations are append-only and validated before traffic.
4. Deploy a candidate without traffic. Require readiness and browser/API smoke tests.
5. Move traffic gradually when the platform supports it. Watch server errors, p95/p99,
   authenticated write success and model errors for at least 30 minutes.

## Rollback

Stop cutover on failed readiness, migration validation, elevated errors, unsafe model
evaluation or latency thresholds. Redeploy the recorded known-good image and its exact
configuration. Never run Flyway repair or reverse a migration in place. If a migration
is incompatible, remove traffic and restore a fresh database from the pre-release backup.

Verify direct and proxied readiness, authentication, one owner-scoped read, one
idempotent write/replay and the offline coach error. Record recovery time and data gap.
The local rehearsal deliberately rejects an invalid candidate before cutover and then
proves the known-good image recovers:

```powershell
./scripts/release/test-rollback.ps1
```
