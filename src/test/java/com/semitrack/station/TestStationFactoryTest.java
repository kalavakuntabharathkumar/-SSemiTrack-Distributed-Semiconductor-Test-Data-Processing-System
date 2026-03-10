package com.semitrack.station;

import com.semitrack.aggregator.AggregatorService;
import com.semitrack.factory.TestStationFactory;
import com.semitrack.model.StationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TestStationFactory}.
 *
 * Verifies that the factory returns the correct concrete subtype for each
 * {@link StationType} and that the station ID is propagated correctly.
 */
class TestStationFactoryTest {

    private final AggregatorService aggregator = new AggregatorService();

    @Test
    @DisplayName("Factory creates ParametricTestStation for PARAMETRIC type")
    void factoryCreatesParametricStation() {
        TestStation station = TestStationFactory.create(StationType.PARAMETRIC, "ST-P1", aggregator);
        assertNotNull(station);
        assertInstanceOf(ParametricTestStation.class, station);
        assertEquals("ST-P1", station.getStationId());
    }

    @Test
    @DisplayName("Factory creates BinaryTestStation for BINARY type")
    void factoryCreatesBinaryStation() {
        TestStation station = TestStationFactory.create(StationType.BINARY, "ST-B1", aggregator);
        assertNotNull(station);
        assertInstanceOf(BinaryTestStation.class, station);
        assertEquals("ST-B1", station.getStationId());
    }

    @Test
    @DisplayName("Factory produces distinct instances for repeated calls with the same type")
    void factoryProducesDistinctInstances() {
        TestStation a = TestStationFactory.create(StationType.PARAMETRIC, "ST-A", aggregator);
        TestStation b = TestStationFactory.create(StationType.PARAMETRIC, "ST-B", aggregator);
        assertNotSame(a, b);
        assertNotEquals(a.getStationId(), b.getStationId());
    }

    @Test
    @DisplayName("Factory propagates station ID to the created station")
    void factoryPropagatesStationId() {
        String id = "STATION-42";
        TestStation station = TestStationFactory.create(StationType.BINARY, id, aggregator);
        assertEquals(id, station.getStationId());
    }
}
