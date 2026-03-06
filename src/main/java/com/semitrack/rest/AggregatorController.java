package com.semitrack.rest;

import com.semitrack.aggregator.AggregationStore;
import com.semitrack.aggregator.AggregatorService;
import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferTestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Boot REST API for the SemiTrack aggregation store.
 *
 * <p>All read operations use Java 8+ streams and lambdas for filtering,
 * mapping, and aggregating results from the shared store.  No write endpoints
 * are exposed here; test stations submit data via the internal blocking queue.
 */
@RestController
@RequestMapping("/api/v1")
public class AggregatorController {

    private final AggregationStore store;

    public AggregatorController(AggregatorService aggregatorService) {
        this.store = aggregatorService.getStore();
    }

    // ── GET /api/v1/results ────────────────────────────────────────────────

    /**
     * Return all test results, optionally filtered by status.
     *
     * @param status optional filter: PASS | FAIL | UNKNOWN
     */
    @GetMapping("/results")
    public ResponseEntity<List<ResultDto>> getAllResults(
            @RequestParam(required = false) String status) {

        List<WaferTestResult> all = store.allResults();

        List<ResultDto> dto = all.stream()
                .filter(r -> status == null ||
                             r.getStatus().name().equalsIgnoreCase(status))
                .map(ResultDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    // ── GET /api/v1/results/{waferId} ─────────────────────────────────────

    @GetMapping("/results/{waferId}")
    public ResponseEntity<List<ResultDto>> getWaferResults(@PathVariable String waferId) {
        List<ResultDto> dto = store.resultsForWafer(waferId).stream()
                .map(ResultDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    // ── GET /api/v1/summary ───────────────────────────────────────────────

    /**
     * Aggregated pass/fail summary per wafer, including yield percentage.
     */
    @GetMapping("/summary")
    public ResponseEntity<List<WaferSummaryDto>> getSummary() {
        Map<String, long[]> raw = store.summaryByWafer();

        List<WaferSummaryDto> summaries = raw.entrySet().stream()
                .map(e -> {
                    long pass  = e.getValue()[0];
                    long fail  = e.getValue()[1];
                    long total = pass + fail;
                    double yield = total > 0 ? 100.0 * pass / total : 0.0;
                    return new WaferSummaryDto(e.getKey(), pass, fail, yield);
                })
                .sorted(Comparator.comparing(WaferSummaryDto::waferId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    // ── GET /api/v1/stats ─────────────────────────────────────────────────

    /**
     * High-level statistics across all wafers.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<WaferTestResult> all = store.allResults();

        long passCount = all.stream().filter(r -> TestStatus.PASS.equals(r.getStatus())).count();
        long failCount = all.stream().filter(r -> TestStatus.FAIL.equals(r.getStatus())).count();
        long total     = all.size();
        double overallYield = total > 0 ? 100.0 * passCount / total : 0.0;

        OptionalDouble avgVoltage = all.stream()
                .mapToDouble(r -> r.getParametricValues().getOrDefault("voltage", 0.0))
                .filter(v -> v > 0)
                .average();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalResults",  total);
        stats.put("passCount",     passCount);
        stats.put("failCount",     failCount);
        stats.put("overallYield",  String.format("%.2f%%", overallYield));
        stats.put("avgVoltage",    avgVoltage.isPresent()
                                   ? String.format("%.4f V", avgVoltage.getAsDouble())
                                   : "N/A");
        stats.put("waferCount",    store.waferIds().size());

        return ResponseEntity.ok(stats);
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record ResultDto(
            String stationId,
            String waferId,
            int dieX,
            int dieY,
            String status,
            Map<String, Double> parametricValues,
            String timestamp) {

        static ResultDto from(WaferTestResult r) {
            return new ResultDto(
                    r.getStationId(),
                    r.getWaferId(),
                    r.getDie().getX(),
                    r.getDie().getY(),
                    r.getStatus().name(),
                    r.getParametricValues(),
                    r.getTimestamp().toString());
        }
    }

    public record WaferSummaryDto(
            String waferId,
            long passCount,
            long failCount,
            double yieldPct) {}
}
