package com.semitrack.rmi;

import com.semitrack.aggregator.AggregationStore;
import com.semitrack.config.AppConfig;
import com.semitrack.logger.SemiTrackLogger;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Utility that registers the {@link TestResultServiceImpl} in the local RMI registry.
 *
 * <p>Call {@link #start(AggregationStore)} once after the aggregation service
 * is ready.  The registry runs in a daemon thread managed by the JVM, so it
 * stays alive for the lifetime of the Spring Boot process.
 */
public final class RmiServer {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    private RmiServer() {}

    /**
     * Binds the result service to the RMI registry.
     *
     * @param store the shared aggregation store to expose
     */
    public static void start(AggregationStore store) {
        int port = AppConfig.getInstance().getRmiPort();
        try {
            TestResultService stub = new TestResultServiceImpl(store);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("TestResultService", stub);
            LOG.info("[RMI] Server bound to registry on port " + port);
        } catch (Exception e) {
            LOG.error("[RMI] Failed to start RMI server: " + e.getMessage());
        }
    }
}
