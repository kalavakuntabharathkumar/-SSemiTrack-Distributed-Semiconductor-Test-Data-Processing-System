package com.semitrack;

import com.semitrack.aggregator.AggregatorService;
import com.semitrack.factory.TestStationFactory;
import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.StationType;
import com.semitrack.observer.AlertObserver;
import com.semitrack.station.TestStation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for SemiTrack – Distributed Semiconductor Test Data Processing System.
 *
 * Bootstraps Spring Boot and kicks off the multi-threaded test station simulation.
 */
@SpringBootApplication
public class SemiTrackApplication {

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(SemiTrackApplication.class, args);

        SemiTrackLogger log = SemiTrackLogger.getInstance();
        AggregatorService aggregator = ctx.getBean(AggregatorService.class);

        // Register observers (Observer pattern)
        aggregator.addObserver(new AlertObserver());

        // Create test stations via factory (Factory pattern)
        List<TestStation> stations = List.of(
                TestStationFactory.create(StationType.PARAMETRIC, "ST-01", aggregator),
                TestStationFactory.create(StationType.PARAMETRIC, "ST-02", aggregator),
                TestStationFactory.create(StationType.BINARY,     "ST-03", aggregator)
        );

        // Run stations concurrently (multi-threaded)
        ExecutorService pool = Executors.newFixedThreadPool(stations.size());
        for (TestStation station : stations) {
            pool.submit(station::runTests);
        }

        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);
        if (!finished) {
            log.warn("Some test stations did not finish within the timeout window.");
        } else {
            log.info("All test stations finished. Aggregated " +
                     aggregator.getStore().totalResults() + " results.");
        }
    }
}
