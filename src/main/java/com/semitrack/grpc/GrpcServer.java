package com.semitrack.grpc;

import com.semitrack.aggregator.AggregationStore;
import com.semitrack.config.AppConfig;
import com.semitrack.logger.SemiTrackLogger;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * Starts a gRPC server that exposes the {@link TestResultGrpcService}.
 *
 * <p>The server runs on a configurable port (default 9090) in a non-daemon
 * background thread so it stays alive alongside the Spring Boot HTTP server.
 */
public final class GrpcServer {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    private GrpcServer() {}

    /**
     * Builds and starts the gRPC server.
     *
     * @param store the shared aggregation store to serve
     */
    public static void start(AggregationStore store) {
        int port = AppConfig.getInstance().getGrpcPort();
        try {
            Server server = ServerBuilder.forPort(port)
                    .addService(new TestResultGrpcService(store))
                    .build()
                    .start();
            LOG.info("[gRPC] Server started on port " + port);

            // Add shutdown hook so the server drains cleanly on JVM exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("[gRPC] Shutting down gRPC server …");
                server.shutdown();
            }, "grpc-shutdown"));
        } catch (Exception e) {
            LOG.error("[gRPC] Failed to start gRPC server: " + e.getMessage());
        }
    }
}
