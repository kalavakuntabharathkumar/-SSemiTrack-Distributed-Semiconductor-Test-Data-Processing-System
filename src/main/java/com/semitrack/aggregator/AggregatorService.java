package com.semitrack.aggregator;

import com.semitrack.config.AppConfig;
import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferTestResult;
import com.semitrack.observer.BatchFailureObserver;
import com.semitrack.validator.CoordinateValidator;
import com.semitrack.validator.RangeValidator;
import com.semitrack.validator.Validator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Central aggregation service.
 *
 * <p>Test stations push {@link WaferTestResult} objects onto a
 * {@link LinkedBlockingQueue}.  A dedicated consumer thread drains the queue,
 * runs all {@link Validator validators}, and – if valid – commits the result to
 * the {@link AggregationStore}.  Batch observer notifications fire whenever a
 * full batch has been committed.
 *
 * <p>Using a {@code BlockingQueue} here simulates inter-process communication
 * (IPC) via a bounded buffer: producers block when the queue is full and the
 * consumer blocks when it is empty, without any explicit {@code wait/notify}
 * boilerplate.
 */
@Service
public class AggregatorService {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();
    private static final int QUEUE_CAPACITY = 512;

    private final AggregationStore store;
    private final BlockingQueue<WaferTestResult> queue;
    private final List<Validator> validators;
    private final List<BatchFailureObserver> observers;
    private final AppConfig config;

    public AggregatorService() {
        this.store     = new AggregationStore();
        this.queue     = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.validators = new ArrayList<>();
        this.observers  = new ArrayList<>();
        this.config     = AppConfig.getInstance();

        // Default validators
        validators.add(new CoordinateValidator(config.getWaferGridMaxX(), config.getWaferGridMaxY()));
        validators.add(new RangeValidator("voltage",   0.0, 5.0));
        validators.add(new RangeValidator("threshold", 0.0, 3.3));

        // Start the consumer thread
        Thread consumer = new Thread(this::consume, "aggregator-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    // ── Producer API ─────────────────────────────────────────────────────────

    /**
     * Called by test stations to submit a result.  Blocks if the queue is full.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void submit(WaferTestResult result) throws InterruptedException {
        queue.put(result);
    }

    // ── Consumer ─────────────────────────────────────────────────────────────

    private void consume() {
        List<WaferTestResult> batch = new ArrayList<>();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Block until at least one result is available
                WaferTestResult head = queue.take();
                batch.add(head);
                queue.drainTo(batch);   // collect any extras already in queue

                for (WaferTestResult result : batch) {
                    if (isValid(result)) {
                        store.add(result);
                    } else {
                        LOG.warn("Rejected invalid result: " + result);
                    }
                }

                notifyObserversIfNeeded(batch);
                batch.clear();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isValid(WaferTestResult result) {
        return validators.stream().allMatch(v -> v.validate(result));
    }

    // ── Observer management ───────────────────────────────────────────────────

    public synchronized void addObserver(BatchFailureObserver observer) {
        observers.add(observer);
    }

    private void notifyObserversIfNeeded(List<WaferTestResult> batch) {
        long failCount = batch.stream()
                              .filter(r -> TestStatus.FAIL.equals(r.getStatus()))
                              .count();
        double failureRate = (double) failCount / batch.size();

        if (failureRate >= config.getBatchFailureThreshold()) {
            List<BatchFailureObserver> snapshot;
            synchronized (this) { snapshot = new ArrayList<>(observers); }
            for (BatchFailureObserver obs : snapshot) {
                obs.onBatchFailure(batch, failureRate);
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public AggregationStore getStore()              { return store; }
    public List<Validator> getValidators()          { return validators; }
}
