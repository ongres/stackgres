package io.stackgres.systemtest;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Minimal "wait until" for convergence assertions (create→HEALTHY, delete→gone) — no extra dependency. */
public final class Poll {

    private Poll() {
    }

    public static void until(Duration timeout, Duration interval, String description, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        AssertionError last = null;
        while (System.nanoTime() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
                last = null;
            } catch (AssertionError e) {
                last = e;   // transient (e.g. not-found yet) — keep polling, rethrow on timeout
            }
            try {
                Thread.sleep(interval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for: " + description, e);
            }
        }
        throw new AssertionError("timed out after " + timeout + " waiting for: " + description, last);
    }
}
