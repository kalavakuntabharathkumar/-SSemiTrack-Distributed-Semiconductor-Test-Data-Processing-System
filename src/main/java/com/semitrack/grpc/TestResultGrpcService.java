package com.semitrack.grpc;

import com.semitrack.aggregator.AggregationStore;
import com.semitrack.grpc.proto.*;
import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Map;

/**
 * gRPC service implementation for SemiTrack test result queries.
 *
 * <p>Extends the protobuf-generated {@code TestResultServiceGrpc.TestResultServiceImplBase}
 * and overrides each RPC method to read from the shared {@link AggregationStore}.
 *
 * <p>Unlike RMI, gRPC uses HTTP/2 as transport, supports language-agnostic
 * clients, and handles streaming natively – as shown by {@link #streamResults}.
 */
public class TestResultGrpcService
        extends TestResultServiceGrpc.TestResultServiceImplBase {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();
    private final AggregationStore store;

    public TestResultGrpcService(AggregationStore store) {
        this.store = store;
    }

    // ── Unary RPCs ────────────────────────────────────────────────────────────

    @Override
    public void getAllResults(GetAllResultsRequest request,
                              StreamObserver<GetAllResultsResponse> responseObserver) {
        LOG.info("[gRPC] getAllResults() invoked");
        List<WaferTestResult> results = store.allResults();

        GetAllResultsResponse.Builder responseBuilder = GetAllResultsResponse.newBuilder()
                .setTotalCount(results.size());
        results.stream()
               .map(this::toProto)
               .forEach(responseBuilder::addResults);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getWaferResults(GetWaferResultsRequest request,
                                StreamObserver<GetWaferResultsResponse> responseObserver) {
        LOG.info("[gRPC] getWaferResults(" + request.getWaferId() + ") invoked");
        List<WaferTestResult> results = store.resultsForWafer(request.getWaferId());

        GetWaferResultsResponse.Builder rb = GetWaferResultsResponse.newBuilder();
        results.stream().map(this::toProto).forEach(rb::addResults);

        responseObserver.onNext(rb.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getSummary(GetSummaryRequest request,
                           StreamObserver<GetSummaryResponse> responseObserver) {
        LOG.info("[gRPC] getSummary() invoked");
        Map<String, long[]> summaryMap = store.summaryByWafer();

        GetSummaryResponse.Builder rb = GetSummaryResponse.newBuilder();
        long totalPass = 0, totalFail = 0;

        for (Map.Entry<String, long[]> entry : summaryMap.entrySet()) {
            long pass = entry.getValue()[0];
            long fail = entry.getValue()[1];
            long total = pass + fail;
            double yieldPct = total > 0 ? 100.0 * pass / total : 0.0;

            rb.addSummaries(WaferSummary.newBuilder()
                    .setWaferId(entry.getKey())
                    .setPassCount(pass)
                    .setFailCount(fail)
                    .setYieldPct(yieldPct)
                    .build());
            totalPass += pass;
            totalFail += fail;
        }

        rb.setTotalPass(totalPass).setTotalFail(totalFail);
        responseObserver.onNext(rb.build());
        responseObserver.onCompleted();
    }

    // ── Server streaming RPC ──────────────────────────────────────────────────

    @Override
    public void streamResults(GetAllResultsRequest request,
                              StreamObserver<com.semitrack.grpc.proto.WaferTestResult> responseObserver) {
        LOG.info("[gRPC] streamResults() invoked");
        List<WaferTestResult> results = store.allResults();
        for (WaferTestResult r : results) {
            responseObserver.onNext(toProto(r));
        }
        responseObserver.onCompleted();
    }

    // ── Conversion helper ─────────────────────────────────────────────────────

    private com.semitrack.grpc.proto.WaferTestResult toProto(WaferTestResult r) {
        WaferDie die = r.getDie();
        return com.semitrack.grpc.proto.WaferTestResult.newBuilder()
                .setStationId(r.getStationId())
                .setWaferId(r.getWaferId())
                .setDie(com.semitrack.grpc.proto.WaferDie.newBuilder()
                            .setX(die.getX()).setY(die.getY()).build())
                .setStatus(mapStatus(r.getStatus()))
                .putAllParametricValues(r.getParametricValues())
                .setTimestamp(r.getTimestamp().toString())
                .build();
    }

    private TestStatus mapStatus(com.semitrack.model.TestStatus s) {
        return switch (s) {
            case PASS    -> TestStatus.TEST_STATUS_PASS;
            case FAIL    -> TestStatus.TEST_STATUS_FAIL;
            case UNKNOWN -> TestStatus.TEST_STATUS_UNKNOWN;
        };
    }
}
