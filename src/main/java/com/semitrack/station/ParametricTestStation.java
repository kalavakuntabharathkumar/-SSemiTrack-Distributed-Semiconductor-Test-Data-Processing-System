package com.semitrack.station;

import com.semitrack.aggregator.AggregatorService;
import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a parametric test station that measures continuous electrical
 * parameters (voltage, threshold current) for each die.
 *
 * <p>A die is marked PASS if both its voltage and threshold fall within the
 * nominal operating window; otherwise it is FAIL.  A small percentage of dies
 * are intentionally generated with out-of-range values to exercise the
 * {@link com.semitrack.validator.RangeValidator} and the fail-rate observer.
 */
public class ParametricTestStation extends TestStation {

    // Nominal pass windows
    private static final double VOLTAGE_MIN   = 1.1;
    private static final double VOLTAGE_MAX   = 1.4;
    private static final double THRESHOLD_MIN = 0.4;
    private static final double THRESHOLD_MAX = 0.7;

    // Probability that a given die will be generated as defective
    private static final double DEFECT_RATE = 0.15;

    public ParametricTestStation(String stationId, AggregatorService aggregator) {
        super(stationId, aggregator);
    }

    @Override
    protected List<WaferTestResult> generateResults(String waferId) {
        List<WaferTestResult> results = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int diesPerWafer = config.getDiesPerWafer();

        // Use a TreeSet to guarantee unique, sorted die coordinates
        Set<WaferDie> dieSet = new TreeSet<>();
        while (dieSet.size() < diesPerWafer) {
            int x = rng.nextInt(0, config.getWaferGridMaxX() + 1);
            int y = rng.nextInt(0, config.getWaferGridMaxY() + 1);
            dieSet.add(new WaferDie(x, y));
        }

        for (WaferDie die : dieSet) {
            boolean defective = rng.nextDouble() < DEFECT_RATE;

            double voltage   = defective
                    ? rng.nextDouble(0.0, VOLTAGE_MIN)        // deliberately bad
                    : rng.nextDouble(VOLTAGE_MIN, VOLTAGE_MAX);

            double threshold = defective
                    ? rng.nextDouble(THRESHOLD_MAX, 3.3)      // deliberately bad
                    : rng.nextDouble(THRESHOLD_MIN, THRESHOLD_MAX);

            TestStatus status = (!defective &&
                                 voltage   >= VOLTAGE_MIN   && voltage   <= VOLTAGE_MAX &&
                                 threshold >= THRESHOLD_MIN && threshold <= THRESHOLD_MAX)
                    ? TestStatus.PASS : TestStatus.FAIL;

            Map<String, Double> params = new HashMap<>();
            params.put("voltage",   voltage);
            params.put("threshold", threshold);

            results.add(new WaferTestResult(getStationId(), waferId, die, status, params));
        }
        return results;
    }
}
