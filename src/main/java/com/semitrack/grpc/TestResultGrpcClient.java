package com.semitrack.grpc;

import com.semitrack.config.AppConfig;
import com.semitrack.grpc.proto.*;
import com.semitrack.logger.SemiTrackLogger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Demonstration gRPC client for the SemiTrack {@link TestResultGrpcService}.
 *
 * <p>Uses a blocking stub for simplicity; in production an async stub would
 * be preferred to avoid blocking the caller's thread during network I/O.
 *
 * <h3>Key gRPC advantages over RMI demonstrated here</h3>
 * <ul>
 *   <li>Language-agnostic: any language with a protobuf plugin can use the same
 *       {@code .proto} schema.</li>
 *   <li>HTTP/2 transport: multiplexed, header-compressed, supports streaming.</li>
 *   <li>Robust versioning: adding a field with a new number is backward-compatible;
 *       unknown fields are preserved, not dropped or cause exceptions.</li>
 * </ul>
 */
public final class TestResultGrpcClient {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    private TestResultGrpcClient() {}

    /**
     * Fetches all test results from the local gRPC server and prints a summary.
     */
    public static void fetchAndPrintSummary() {
        int port = AppConfig.getInstance().getGrpcPort();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();

        try {
            TestResultServiceGrpc.TestResultServiceBlockingStub stub =
                    TestResultServiceGrpc.newBlockingStub(channel);

            GetSummaryResponse response = stub.getSummary(GetSummaryRequest.newBuilder().build());
            LOG.info("[gRPC Client] Summary — total PASS=" + response.getTotalPass()
                     + " FAIL=" + response.getTotalFail());

            for (WaferSummary ws : response.getSummariesList()) {
                LOG.info(String.format("[gRPC Client]   Wafer %-12s PASS=%3d FAIL=%3d Yield=%.1f%%",
                        ws.getWaferId(), ws.getPassCount(), ws.getFailCount(), ws.getYieldPct()));
            }
        } catch (Exception e) {
            LOG.error("[gRPC Client] Error: " + e.getMessage());
        } finally {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
