# Database Recovery Runbook

## Trigger And Containment

Use this runbook for failed MySQL readiness, corruption, accidental deletion or an
unrecoverable migration. Remove the backend from traffic, preserve logs and database
metadata, and stop writes. Redis is not a recovery source.

## Restore

1. Declare incident time and select the newest encrypted backup at or before that time.
   Verify its checksum and record the expected RPO gap.
2. Provision a fresh MySQL 8 volume with credentials from the secret manager. Never
   restore over the failed volume.
3. Import the backup, then run Flyway validation without repair.
4. Compare counts and ownership invariants for accounts, profiles, assessments, plans,
   tracking, conversations, knowledge, audit events and schema history.
5. Start one offline backend candidate against the restored database and Redis. Verify
   liveness, readiness, authenticated reads and one bounded write/replay.
6. Cut traffic only after the incident commander and database recovery owner approve.

The executable local drill is:

```powershell
./scripts/recovery/test-mysql-restore.ps1
```

It must report RPO at most 60 minutes, RTO at most 14,400 seconds, a fresh volume,
zero count differences and no retained raw dump. After recovery, rotate database
credentials, preserve only non-sensitive evidence and document any unrecoverable gap.
