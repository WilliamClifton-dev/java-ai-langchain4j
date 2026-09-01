# Data Retention And Backup Policy

## Scope And Authority

This is the exact L1 public-beta policy. MySQL is the durable source of truth. Redis
contains only expiring or reconstructable state. A deployment that cannot enforce the
backup schedule, expiry, encryption and access controls below must not accept public
traffic.

## Retention Schedule

| Data | Retention and deletion rule |
|---|---|
| Active account, profile, HBTI attempts, plans, tracking, reviews and coach messages | retained until the user invokes account deletion; no inactive-account auto-deletion in L1 |
| Account deletion | synchronous hard deletion of the account and owned rows; retained audit rows are anonymized immediately |
| Refresh-token hashes | usable for at most 30 days; expired rows are deleted after a 7-day replay-investigation grace period by the daily cleanup job |
| Audit events | 180 days, then deleted by the daily cleanup job |
| Redis counters, leases and HBTI cache | existing configured TTLs: login 15 minutes, coach 1 minute, lease at most 5 minutes, public definition cache 1 hour |
| Application logs | 30 days in the external log platform; logs contain no bodies, tokens, prompts, model output or health facts |
| Aggregate metrics | 90 days; labels must remain bounded and must not contain user identity or content |
| Release evaluation, load, restore and rollback reports | 180 days; evidence must not contain credentials, raw model conversations or database dumps |
| HBTI definitions and reviewed knowledge provenance | retained while a version is supported and after retirement when needed to interpret stored results; contains no user data |

The application runs `RetentionCleanupJob` every 24 hours after a one-hour startup
delay. `RETENTION_CLEANUP_ENABLED` must remain true for public beta. The job deletes
refresh-token rows whose expiry is more than seven days old and audit events older
than 180 days. Account deletion remains immediate and does not wait for this job.

If the beta closes, operators give 30 days' notice and remove the primary database no
later than 30 days after closure. Backups then age out under the schedule below.

## Backup Schedule And Expiry

- Create an encrypted, off-host, single-transaction logical backup every hour.
- Retain hourly backups for 48 hours.
- Retain the 00:00 UTC daily backup for 35 days.
- Permanently expire all copies after 35 days; no legal hold is assumed for this beta.
- Restrict decrypt/restore access to the database recovery role and record every use.
- Do not place database dumps in CI artifacts, source control or ordinary application storage.

This schedule sets the RPO objective to one hour. The RTO objective is four hours.
`scripts/recovery/test-mysql-restore.ps1` restores a consistent dump into a fresh
volume, compares 21 durable table invariants and destroys the raw dump after hashing.
Account data deleted from the primary store can remain in inaccessible backups for at
most 35 days and must not be selectively restored into the live service.

## Verification

Review daily cleanup logs for `retention_cleanup_completed` and alert if no successful
run occurs within 30 hours. Run the fresh-volume restore drill before each release and
at least monthly while the beta is open. A count-only drill is recovery evidence, not
a substitute for encrypted off-host backup configuration on the deployment platform.
