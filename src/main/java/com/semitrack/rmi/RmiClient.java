package com.semitrack.rmi;

import com.semitrack.config.AppConfig;
import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.WaferTestResult;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;

/**
 * Demonstration RMI client that fetches test results from the RMI registry.
 *
 * <p>In production this would run in a separate JVM or a remote engineer's
 * workstation.  Here it is kept in the same project for demonstration and
 * integration-test purposes.
 *
 * <h3>RMI vs gRPC trade-offs (see also README)</h3>
 * <table border="1">
 *   <tr><th>Aspect</th><th>Java RMI</th><th>gRPC + Protobuf</th></tr>
 *   <tr><td>Language</td><td>Java only</td><td>Polyglot</td></tr>
 *   <tr><td>Schema</td><td>Java interfaces + Serializable</td><td>.proto IDL</td></tr>
 *   <tr><td>Transport</td><td>JRMP (TCP)</td><td>HTTP/2</td></tr>
 *   <tr><td>Versioning</td><td>serialVersionUID, fragile</td><td>field numbers, robust</td></tr>
 *   <tr><td>Streaming</td><td>Not supported</td><td>Server/client/bidi streaming</td></tr>
 *   <tr><td>Performance</td><td>Java serialization overhead</td><td>Binary protobuf, fast</td></tr>
 * </table>
 */
public final class RmiClient {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    private RmiClient() {}

    /**
     * Connects to the local RMI registry and prints the total result count.
     *
     * @return list of all results, or empty list on error
     */
    public static List<WaferTestResult> fetchAllResults() {
        int port = AppConfig.getInstance().getRmiPort();
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", port);
            TestResultService service =
                    (TestResultService) registry.lookup("TestResultService");
            List<WaferTestResult> results = service.getAllResults();
            LOG.info("[RMI Client] Received " + results.size() + " results via RMI.");
            return results;
        } catch (Exception e) {
            LOG.error("[RMI Client] Failed to fetch results: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
