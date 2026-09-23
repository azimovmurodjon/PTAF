package com.ptaf.ui_performance.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates a simultaneous first browser journey across configured UI performance virtual users.
 *
 * <p>Each virtual user launches its independent browser first, marks itself ready, and waits here.
 * The engine releases all ready users together only after every requested user has either prepared
 * its browser or reported a browser-start failure. This keeps browser startup time out of measured
 * journey timing while making the first configured action concurrent.</p>
 */
public final class UiPerformanceStartGate {
    private final CountDownLatch usersReady;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final AtomicBoolean released = new AtomicBoolean(false);

    /** Creates a gate for exactly the configured number of virtual users. */
    public UiPerformanceStartGate(int virtualUsers) {
        if (virtualUsers < 1) {
            throw new IllegalArgumentException("UI performance simultaneous start requires at least one virtual user.");
        }
        this.usersReady = new CountDownLatch(virtualUsers);
    }

    /** Records that one user browser is ready to begin, or that its startup attempt has completed with a failure. */
    public void markVirtualUserReady() {
        usersReady.countDown();
    }

    /** Blocks an individual prepared virtual user until the engine releases the common start signal. */
    public void awaitStartSignal() {
        try {
            startSignal.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("UI performance virtual user was interrupted while waiting for simultaneous start.", exception);
        }
    }

    /** Waits until every configured user has completed browser preparation. */
    public void awaitAllUsers(long timeoutMs) {
        if (timeoutMs < 1) {
            throw new IllegalArgumentException("UI performance simultaneous start timeout must be at least 1 ms.");
        }
        try {
            boolean allUsersPrepared = usersReady.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!allUsersPrepared) {
                release();
                throw new IllegalStateException(
                        "UI performance simultaneous start timed out before all configured virtual users prepared their browsers. "
                                + "Increase ui_performance.execution.synchronized_start_timeout_ms or reduce virtual_users for this machine.");
            }
        } catch (InterruptedException exception) {
            release();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("UI performance simultaneous start was interrupted.", exception);
        }
    }

    /** Backward-compatible convenience that waits for preparation and releases all users together. */
    public void awaitAllUsersThenRelease(long timeoutMs) {
        awaitAllUsers(timeoutMs);
        release();
    }

    /** Releases waiting users exactly once. */
    public void release() {
        if (released.compareAndSet(false, true)) {
            startSignal.countDown();
        }
    }
}
