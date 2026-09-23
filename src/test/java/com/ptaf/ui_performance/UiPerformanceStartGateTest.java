package com.ptaf.ui_performance;

import com.ptaf.ui_performance.core.UiPerformanceStartGate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Offline concurrency contract for the UI performance shared start gate. */
public class UiPerformanceStartGateTest {

    @Test
    public void releasesPreparedVirtualUsersTogetherOnlyAfterEveryUserIsReady() throws Exception {
        UiPerformanceStartGate gate = new UiPerformanceStartGate(2);
        CountDownLatch usersReachedGate = new CountDownLatch(2);
        AtomicInteger releasedUsers = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> waitAtGate(gate, usersReachedGate, releasedUsers));
            Future<?> second = executor.submit(() -> waitAtGate(gate, usersReachedGate, releasedUsers));

            Assert.assertTrue(usersReachedGate.await(1, TimeUnit.SECONDS), "Both virtual users must prepare before release.");
            Assert.assertEquals(releasedUsers.get(), 0, "No virtual user may begin before the common start signal.");

            gate.awaitAllUsersThenRelease(1_000L);
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
            Assert.assertEquals(releasedUsers.get(), 2, "The common start signal must release every prepared virtual user.");
        } finally {
            executor.shutdownNow();
        }
    }

    private void waitAtGate(UiPerformanceStartGate gate, CountDownLatch usersReachedGate, AtomicInteger releasedUsers) {
        gate.markVirtualUserReady();
        usersReachedGate.countDown();
        gate.awaitStartSignal();
        releasedUsers.incrementAndGet();
    }
}
