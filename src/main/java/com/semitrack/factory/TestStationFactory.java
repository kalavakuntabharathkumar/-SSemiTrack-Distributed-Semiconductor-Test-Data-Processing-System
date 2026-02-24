package com.semitrack.factory;

import com.semitrack.aggregator.AggregatorService;
import com.semitrack.model.StationType;
import com.semitrack.station.BinaryTestStation;
import com.semitrack.station.ParametricTestStation;
import com.semitrack.station.TestStation;

/**
 * Factory class for creating {@link TestStation} instances.
 *
 * <p>Encapsulates the construction logic so that callers never need to know
 * which concrete class backs a given {@link StationType}.  Adding a new
 * station type requires only:
 * <ol>
 *   <li>A new entry in {@link StationType}.</li>
 *   <li>A new {@code case} branch here.</li>
 * </ol>
 *
 * No changes are required in the simulation runner ({@code SemiTrackApplication})
 * or any other component – demonstrating the Open/Closed Principle.
 */
public final class TestStationFactory {

    /** Utility class – prevent instantiation. */
    private TestStationFactory() {}

    /**
     * Creates and returns a {@link TestStation} of the requested type.
     *
     * @param type       the station variant to instantiate
     * @param stationId  unique identifier for this station (e.g. "ST-01")
     * @param aggregator the shared aggregation service the station will report to
     * @return a fully initialised {@code TestStation}
     * @throws IllegalArgumentException if {@code type} is not handled
     */
    public static TestStation create(StationType type,
                                     String stationId,
                                     AggregatorService aggregator) {
        return switch (type) {
            case PARAMETRIC -> new ParametricTestStation(stationId, aggregator);
            case BINARY     -> new BinaryTestStation(stationId, aggregator);
        };
    }
}
