package com.atguigu.java.ai.langchain4j.coach.streaming;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModelCircuitBreaker {
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private State state = State.CLOSED;
    private int failures;
    private Instant reopenAt = Instant.MIN;

    public ModelCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        if (failureThreshold < 1 || openDuration == null || openDuration.isNegative()
                || openDuration.isZero() || clock == null) {
            throw new IllegalArgumentException("Circuit breaker configuration is invalid");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public synchronized Permit tryAcquire() {
        if (state == State.CLOSED) return Permit.allowed(false);
        if (state == State.OPEN && !clock.instant().isBefore(reopenAt)) {
            state = State.HALF_OPEN;
            return Permit.allowed(true);
        }
        return Permit.rejected();
    }

    public synchronized void onSuccess(Permit permit) {
        if (!permit.resolve()) return;
        failures = 0;
        state = State.CLOSED;
    }

    public synchronized void onFailure(Permit permit) {
        if (!permit.resolve()) return;
        if (permit.probe || ++failures >= failureThreshold) open();
    }

    private void open() {
        state = State.OPEN;
        reopenAt = clock.instant().plus(openDuration);
    }

    private enum State { CLOSED, OPEN, HALF_OPEN }

    public static final class Permit {
        private final boolean allowed;
        private final boolean probe;
        private final AtomicBoolean resolved = new AtomicBoolean();

        private Permit(boolean allowed, boolean probe) {
            this.allowed = allowed;
            this.probe = probe;
        }

        static Permit allowed(boolean probe) {
            return new Permit(true, probe);
        }

        static Permit rejected() {
            Permit permit = new Permit(false, false);
            permit.resolved.set(true);
            return permit;
        }

        public boolean allowed() {
            return allowed;
        }

        public boolean probe() {
            return probe;
        }

        private boolean resolve() {
            return allowed && resolved.compareAndSet(false, true);
        }
    }
}
