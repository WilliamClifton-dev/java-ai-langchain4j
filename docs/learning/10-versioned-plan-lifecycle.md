# Learning Note 10: Versioned Plan Lifecycle

## What This Module Demonstrates

Task 10 turns calculated values into an auditable business aggregate. The main idea is that a plan is not one mutable row: `weight_plan` owns lifecycle identity and the active pointer, while every `weight_plan_version` preserves the targets and the exact inputs that produced them.

## Code Map

- `V6__create_weight_plan_tables.sql`: relational constraints, provenance columns, hashed idempotency uniqueness, and active pointer.
- `WeightPlanService`: transaction boundary and state-machine orchestration.
- `WeightPlanMapper`: parameterized, owner-scoped SQL and account-row locking.
- `WeightPlanController`: JWT-derived identity and REST resource boundary.
- `PlanLifecycleTest`: transition order, replacement, stale provenance, and idempotent replay.
- `PlanActivationConflictTest`: two committed transactions contend for the same user lock and still leave one ACTIVE version.

## Why The Account Row Is Locked

There may not be a `weight_plan` row on the first request, so locking only the plan cannot serialize first creation. The account always exists for an authenticated user. Locking it gives every same-user plan write one stable coordination row, including version allocation and activation.

This is pessimistic concurrency control. It trades same-user write parallelism for a simple invariant. Different users still use different locks.

## Why Provenance Is Persisted

A target such as 1,700-1,900 kcal is meaningless without knowing which profile, screening, assessment, formula, and policy created it. The version stores those identifiers and results. Before validation, confirmation, or activation, the service recomputes and compares them. Changed inputs produce a new draft instead of mutating history.

## Why Idempotency Keys Are Hashed

HTTP clients retry after timeouts. Without idempotency, one click can create multiple drafts or repeat activation. The service hashes the bounded key with SHA-256 and stores only the digest under a unique plan constraint. The digest supports equality lookup but does not preserve the raw client key.

## Interview Questions

1. Why is `SELECT ... FOR UPDATE` used on `user_account` instead of only `weight_plan`?
2. What is the difference between optimistic and pessimistic concurrency control here?
3. Why is an active pointer safer than treating the highest version number as active?
4. Why must profile and safety provenance be checked again at activation time?
5. What makes an idempotent replay different from a duplicate request conflict?
6. Why do mapper writes still include ownership predicates after the service already loaded an owned row?
7. What would need measurement before replacing the account-row lock at higher scale?

## Accurate Resume Boundary

You can claim a transactional, user-owned, versioned plan lifecycle with deterministic provenance, idempotent create/activate operations, and concurrent activation tests. Do not claim high-concurrency or enterprise readiness until Task 23 produces MySQL load and recovery evidence.
