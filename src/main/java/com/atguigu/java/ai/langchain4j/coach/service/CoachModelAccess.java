package com.atguigu.java.ai.langchain4j.coach.service;

import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateGuard;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachRateLimitExceededException;
import com.atguigu.java.ai.langchain4j.coach.streaming.ModelCircuitBreaker;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/** One admission policy shared by synchronous and streaming model calls. */
public final class CoachModelAccess {
    private final CoachRateGuard rateGuard;
    private final ModelCircuitBreaker breaker;
    private final Semaphore concurrency;

    public CoachModelAccess(CoachRateGuard rateGuard, ModelCircuitBreaker breaker, int maxConcurrent) {
        if (rateGuard == null || breaker == null || maxConcurrent < 1) {
            throw new IllegalArgumentException("Model access configuration is invalid");
        }
        this.rateGuard = rateGuard;
        this.breaker = breaker;
        this.concurrency = new Semaphore(maxConcurrent);
    }

    public Permit acquire(String ownerId) {
        try {
            rateGuard.assertAllowed(ownerId);
        } catch (CoachRateLimitExceededException exception) {
            throw new CoachModelException("MODEL_RATE_LIMITED");
        }
        if (!concurrency.tryAcquire()) throw new CoachModelException("MODEL_CONCURRENCY_LIMIT");
        ModelCircuitBreaker.Permit permit = breaker.tryAcquire();
        if (!permit.allowed()) {
            concurrency.release();
            throw new CoachModelException("MODEL_CIRCUIT_OPEN");
        }
        return new Permit(permit);
    }

    public final class Permit implements AutoCloseable {
        private final ModelCircuitBreaker.Permit permit;
        private final AtomicBoolean resolved = new AtomicBoolean();

        private Permit(ModelCircuitBreaker.Permit permit) {
            this.permit = permit;
        }

        public void success() { resolve(() -> breaker.onSuccess(permit)); }
        public void failure() { resolve(() -> breaker.onFailure(permit)); }
        @Override public void close() { resolve(() -> breaker.onCancellation(permit)); }

        private void resolve(Runnable outcome) {
            if (!resolved.compareAndSet(false, true)) return;
            try {
                outcome.run();
            } finally {
                concurrency.release();
            }
        }
    }
}
