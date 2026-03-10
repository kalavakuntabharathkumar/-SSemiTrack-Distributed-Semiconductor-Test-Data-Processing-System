package com.semitrack.aggregator;

import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AggregatorService}.
 *
 * These tests exercise the full submit → validate → store pipeline, including
 * the internal consumer thread and blocking queue, without mocking any
 * production components.
 *
 * Awaitility is used instead of Thread.sleep to avoid flakiness: we poll
 * until the consumer thread has processed the submitted result.
 */
class AggregatorServiceIntegrationTest {

    @Test
    @DisplayName("Valid result submitted to the aggregator appears in the store")
    void validResultAppearsInStore() throws InterruptedException {
        AggregatorService svc = new AggregatorService();
        WaferTestResult result = new WaferTestResult(
                "ST-01", "W-001",
                new WaferDie(5, 5),
                TestStatus.PASS,
                Map.of("voltage", 1.2, "threshold", 0.5));

        svc.submit(result);

        // Poll until the consumer thread has processed and stored the result
        await().atMost(5, TimeUnit.SECONDS)
               .until(() -> svc.getStore().totalResults() == 1);

        assertEquals(1, svc.getStore().totalResults());
    }

    @Test
    @DisplayName("Invalid result (bad coordinates) is rejected by the store")
    void invalidResultIsRejected() throws InterruptedException {
        AggregatorService svc = new AggregatorService();

        // x=999 is outside the 0..24 coordinate grid
        WaferTestResult bad = new WaferTestResult(
                "ST-01", "W-002",
                new WaferDie(999, 0),
                TestStatus.PASS,
                Map.of("voltage", 1.2, "threshold", 0.5));

        svc.submit(bad);

        // Give the consumer time to process
        TimeUnit.MILLISECONDS.sleep(500);
        assertEquals(0, svc.getStore().totalResults(),
                "Out-of-bounds result should be rejected");
    }

    @Test
    @DisplayName("Multiple valid results from different stations all appear in the store")
    void multipleResultsFromDifferentStations() throws InterruptedException {
        AggregatorService svc = new AggregatorService();
        int count = 20;

        for (int i = 0; i < count; i++) {
            svc.submit(new WaferTestResult(
                    "ST-0" + (i % 3),
                    "W-MULTI",
                    new WaferDie(i, 0),
                    TestStatus.PASS,
                    Map.of("voltage", 1.2, "threshold", 0.5)));
        }

        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> svc.getStore().totalResults() == count);

        assertEquals(count, svc.getStore().totalResults());
    }
}
