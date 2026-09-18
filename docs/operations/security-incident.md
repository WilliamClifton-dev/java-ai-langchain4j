# Security Incident Runbook

## Severity And Immediate Actions

Treat credential/token exposure, cross-user access, prompt or secret disclosure,
dependency exploitation and unauthorized database access as security incidents.

1. Stop the affected entry point or switch the model feature offline. Preserve service
   availability only when it does not expand exposure.
2. Record UTC time, release digest, bounded request IDs and affected component. Do not
   copy credentials, tokens, prompts, model content or user health facts into chat or tickets.
3. Revoke affected refresh-token families. For signing-key exposure, remove public
   traffic until an explicit multi-key rotation procedure is deployed; changing the
   single current key invalidates all access tokens.
4. Rotate provider, database and deployment credentials through their secret managers.
5. Preserve immutable infrastructure and audit logs under restricted access. Do not
   preserve raw database dumps outside the backup boundary.
6. Determine affected accounts using owner-scoped audit facts. Notify affected users
   and authorities according to the deployment jurisdiction; this repository does not
   claim a regulated-health compliance program.

## Recovery And Closure

Patch the cause, run backend/frontend/security/AI/Compose gates, rehearse rollback and
deploy by immutable digest. Monitor for recurrence for 24 hours. Close only with a
timeline, scope, containment evidence, user-notification decision and follow-up owner.
Never weaken CSRF, ownership, retention or safety rules as an incident workaround.
