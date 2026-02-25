package com.semitrack.aggregator;

import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe shared store for wafer test results.
 *
 * <h3>Data structure rationale</h3>
 * <ul>
 *   <li>Outer key – wafer ID (String): a {@link TreeMap} keeps wafers sorted
 *       lexicographically, enabling O(log n) lookup and ordered iteration.</li>
 *   <li>Inner key – {@link WaferDie}: a {@link TreeMap} sorted by the die's
 *       natural row-major order so the wafer map can be walked deterministically
 *       (tree traversal semantics as required).</li>
 * </ul>
 *
 * <h3>Concurrency</h3>
 * A {@link ReentrantReadWriteLock} is used instead of blanket {@code synchronized}
 * blocks.  Multiple threads can read concurrently; writes are exclusive.  This
 * was identified through a thread-dump analysis (see README § Concurrency Bug)
 * as the fix for a live-lock observed when every read also acquired a write lock.
 */
public class AggregationStore {

    // TreeMap<waferId, TreeMap<die, result>>
    private final TreeMap<String, TreeMap<WaferDie, WaferTestResult>> store;
    private final ReadWriteLock rwLock;

    public AggregationStore() {
        this.store  = new TreeMap<>();
        this.rwLock = new ReentrantReadWriteLock();
    }

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Adds a validated result to the store.
     *
     * If a result for the same (wafer, die) pair already exists it is
     * overwritten – the latest measurement wins.
     */
    public void add(WaferTestResult result) {
        rwLock.writeLock().lock();
        try {
            store.computeIfAbsent(result.getWaferId(), k -> new TreeMap<>())
                 .put(result.getDie(), result);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of all results across all wafers.
     * Safe to call from multiple threads simultaneously.
     */
    public List<WaferTestResult> allResults() {
        rwLock.readLock().lock();
        try {
            return store.values().stream()
                        .flatMap(m -> m.values().stream())
                        .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns all results for a specific wafer in die-coordinate order.
     * Returns an empty list if the wafer ID is not found.
     */
    public List<WaferTestResult> resultsForWafer(String waferId) {
        rwLock.readLock().lock();
        try {
            TreeMap<WaferDie, WaferTestResult> waferMap = store.get(waferId);
            if (waferMap == null) return Collections.emptyList();
            return new ArrayList<>(waferMap.values());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /** Total number of stored test results. */
    public int totalResults() {
        rwLock.readLock().lock();
        try {
            return store.values().stream().mapToInt(Map::size).sum();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /** Returns a sorted set of all known wafer IDs. */
    public Set<String> waferIds() {
        rwLock.readLock().lock();
        try {
            return new TreeSet<>(store.keySet());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Pass/fail summary per wafer.
     * Returns a map of waferId → [passCount, failCount].
     */
    public Map<String, long[]> summaryByWafer() {
        rwLock.readLock().lock();
        try {
            Map<String, long[]> summary = new TreeMap<>();
            store.forEach((waferId, dies) -> {
                long pass = dies.values().stream()
                                .filter(r -> TestStatus.PASS.equals(r.getStatus())).count();
                long fail = dies.size() - pass;
                summary.put(waferId, new long[]{pass, fail});
            });
            return summary;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
