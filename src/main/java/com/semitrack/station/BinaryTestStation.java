package com.semitrack.station;

import com.semitrack.aggregator.AggregatorService;
import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a binary (go/no-go) test station.
 *
 * <p>Instead of continuous measurements this station performs a functional
 * burn-in test and records only a boolean outcome plus a single
 * {@code leakageCurrent} parametric value used to characterise the failure
 * mode.  The voltage and threshold keys are still populated with nominal
 * defaults so that the shared {@link com.semitrack.validator.RangeValidator}
 * can run without special-casing this station type.
 */
public class BinaryTestStation extends TestStation {

    private static final double PASS_RATE = 0.88;

    public BinaryTestStation(String stationId, AggregatorService aggregator) {
        super(stationId, aggregator);
    }

    @Override
    protected List<WaferTestResult> generateResults(String waferId) {
        List<WaferTestResult> results = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        Set<WaferDie> dieSet = new TreeSet<>();
        while (dieSet.size() < config.getDiesPerWafer()) {
            int x = rng.nextInt(0, config.getWaferGridMaxX() + 1);
            int y = rng.nextInt(0, config.getWaferGridMaxY() + 1);
            dieSet.add(new WaferDie(x, y));
        }

        for (WaferDie die : dieSet) {
            boolean pass = rng.nextDouble() < PASS_RATE;
            TestStatus status = pass ? TestStatus.PASS : TestStatus.FAIL;

            Map<String, Double> params = new HashMap<>();
            // Nominal range-validator-compatible values
            params.put("voltage",       pass ? 1.2 : 0.0);
            params.put("threshold",     pass ? 0.5 : 0.0);
            params.put("leakageCurrent", pass ? rng.nextDouble(0, 1e-9)
                                              : rng.nextDouble(1e-6, 1e-3));
            results.add(new WaferTestResult(getStationId(), waferId, die, status, params));
        }
        return results;
    }
}
