package com.semitrack.aggregator;

import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AggregationStore}.
 *
 * Includes edge-case tests (empty store, duplicate die) and a concurrent
 * write test that verifies thread safety under load – reproducing the
 * scenario described in the README concurrency bug section.
 */
class AggregationStoreTest {

    private AggregationStore store;

    @BeforeEach
    void setUp() {
        store = new AggregationStore();
    }

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Store is empty on creation")
    void emptyStoreHasZeroResults() {
        assertEquals(0, store.totalResults());
        assertTrue(store.allResults().isEmpty());
        assertTrue(store.waferIds().isEmpty());
    }

    @Test
    @DisplayName("Adding one result increases count to 1")
    void addingOneResultIncreasesCount() {
        store.add(makeResult("W001", 0, 0, TestStatus.PASS));
        assertEquals(1, store.totalResults());
    }

    @Test
    @DisplayName("resultsForWafer returns correct results for a known wafer")
    void resultsForWaferReturnsCorrectSubset() {
        store.add(makeResult("W001", 0, 0, TestStatus.PASS));
        store.add(makeResult("W001", 1, 0, TestStatus.FAIL));
        store.add(makeResult("W002", 0, 0, TestStatus.PASS));

        List<WaferTestResult> w1Results = store.resultsForWafer("W001");
        assertEquals(2, w1Results.size());

        List<WaferTestResult> w2Results = store.resultsForWafer("W002");
        assertEquals(1, w2Results.size());
    }

    @Test
    @DisplayName("resultsForWafer returns empty list for unknown wafer")
    void resultsForUnknownWaferIsEmpty() {
        List<WaferTestResult> r = store.resultsForWafer("GHOST");
        assertNotNull(r);
        assertTrue(r.isEmpty());
    }

    @Test
    @DisplayName("Duplicate die overwrites previous result (last write wins)")
    void duplicateDieOverwritesPreviousResult() {
        store.add(makeResult("W001", 3, 5, TestStatus.FAIL));
        store.add(makeResult("W001", 3, 5, TestStatus.PASS)); // overwrite

        List<WaferTestResult> results = store.resultsForWafer("W001");
        assertEquals(1, results.size());
        assertEquals(TestStatus.PASS, results.get(0).getStatus());
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("summaryByWafer returns correct pass/fail counts")
    void summaryByWaferIsCorrect() {
        store.add(makeResult("W001", 0, 0, TestStatus.PASS));
        store.add(makeResult("W001", 1, 0, TestStatus.PASS));
        store.add(makeResult("W001", 2, 0, TestStatus.FAIL));

        Map<String, long[]> summary = store.summaryByWafer();
        assertTrue(summary.containsKey("W001"));
        assertEquals(2L, summary.get("W001")[0]);   // pass
        assertEquals(1L, summary.get("W001")[1]);   // fail
    }

    // ── Results ordered by die coordinate (tree traversal) ───────────────────

    @Test
    @DisplayName("resultsForWafer returns results in row-major die order")
    void resultsAreReturnedInDieCoordinateOrder() {
        store.add(makeResult("W001", 3, 0, TestStatus.PASS));
        store.add(makeResult("W001", 0, 5, TestStatus.PASS));
        store.add(makeResult("W001", 0, 0, TestStatus.PASS));
        store.add(makeResult("W001", 1, 2, TestStatus.PASS));

        List<WaferTestResult> ordered = store.resultsForWafer("W001");
        // Expected order: (0,0), (0,5), (1,2), (3,0)
        assertEquals(new WaferDie(0, 0), ordered.get(0).getDie());
        assertEquals(new WaferDie(0, 5), ordered.get(1).getDie());
        assertEquals(new WaferDie(1, 2), ordered.get(2).getDie());
        assertEquals(new WaferDie(3, 0), ordered.get(3).getDie());
    }

    // ── Concurrency ───────────────────────────────────────────────────────────

    /**
     * Reproduces the concurrency scenario described in the README.
     *
     * 10 threads each write 100 results to the same wafer simultaneously.
     * The test asserts that no data is lost and no exception is thrown,
     * proving the ReadWriteLock prevents races.
     */
    @Test
    @DisplayName("Concurrent writes from multiple threads do not lose or corrupt data")
    void concurrentWritesAreSafe() throws InterruptedException {
        int threadCount  = 10;
        int resultsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    startLatch.await();   // all threads start simultaneously
                    for (int i = 0; i < resultsPerThread; i++) {
                        // Each thread writes unique coordinates to avoid overwrites
                        int x = threadId * resultsPerThread + i;
                        store.add(makeResult("W-CONCURRENT", x, threadId, TestStatus.PASS));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();   // fire!
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Threads did not finish in time");
        pool.shutdown();

        assertEquals(threadCount * resultsPerThread, store.totalResults(),
                "No results should be lost under concurrent writes");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private WaferTestResult makeResult(String waferId, int x, int y, TestStatus status) {
        return new WaferTestResult(
                "ST-TEST", waferId,
                new WaferDie(x, y),
                status,
                Map.of("voltage", 1.2, "threshold", 0.5));
    }
}
