package com.semitrack.rest;

import com.semitrack.aggregator.AggregationStore;
import com.semitrack.aggregator.AggregatorService;
import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferTestResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Thymeleaf controller for the SemiTrack real-time dashboard.
 *
 * <p>Populates the model with aggregated metrics and per-wafer breakdowns
 * so the template can render charts and tables without any client-side data
 * fetching on first load.  The page also wires up a JS fetch loop against
 * the REST API for live refresh.
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final AggregationStore store;

    public DashboardController(AggregatorService aggregatorService) {
        this.store = aggregatorService.getStore();
    }

    @GetMapping
    public String dashboard(Model model) {
        List<WaferTestResult> all = store.allResults();

        long total     = all.size();
        long passCount = all.stream().filter(r -> TestStatus.PASS.equals(r.getStatus())).count();
        long failCount = total - passCount;
        double yield   = total > 0 ? 100.0 * passCount / total : 0.0;

        // Per-wafer data for the bar chart
        Map<String, long[]> summaryMap = store.summaryByWafer();
        List<String>  waferIds   = new ArrayList<>(summaryMap.keySet());
        List<Long>    passCounts = waferIds.stream()
                .map(id -> summaryMap.get(id)[0]).collect(Collectors.toList());
        List<Long>    failCounts = waferIds.stream()
                .map(id -> summaryMap.get(id)[1]).collect(Collectors.toList());

        model.addAttribute("totalResults", total);
        model.addAttribute("passCount",    passCount);
        model.addAttribute("failCount",    failCount);
        model.addAttribute("yieldPct",     String.format("%.1f", yield));
        model.addAttribute("waferIds",     waferIds);
        model.addAttribute("passCounts",   passCounts);
        model.addAttribute("failCounts",   failCounts);
        model.addAttribute("waferCount",   store.waferIds().size());

        return "dashboard";
    }
}
