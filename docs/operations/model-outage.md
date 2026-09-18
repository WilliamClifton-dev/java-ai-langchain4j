# Model Outage Runbook

## Trigger

Use this runbook when model error rate exceeds 5% for 5 minutes, first-token p95 exceeds
3 seconds for 15 minutes, the circuit remains open, the provider reports an incident,
or provider spend approaches its hard limit.

## Response

1. Confirm MySQL and Redis readiness independently. Do not classify a database outage
   as a model outage.
2. Record UTC start time, release commit, provider/model version and affected request
   IDs. Never copy prompt or user message content into the incident record.
3. Set `APP_PROFILE=offline` and redeploy the current image if the provider is unstable,
   unsafe or over budget. Do not change deterministic plan, assessment or tracking data.
4. Verify `/actuator/health/readiness` is `UP`, deterministic Web flows work, and coach
   SSE returns the typed unavailable error rather than fabricated advice.
5. Notify users that coaching text is temporarily unavailable; profile, assessment,
   planning, tracking and review remain available.
6. Re-enable a model only after provider health, the versioned AI evaluation and cost
   evidence pass for the exact prompt bundle and model version.

## Exit

Close the incident only after 30 minutes without a model-error alert. Record duration,
request count, spend, evaluation report hash and whether any unsafe output was reported.
Model outage never authorizes bypassing deterministic safety or tool authorization.
