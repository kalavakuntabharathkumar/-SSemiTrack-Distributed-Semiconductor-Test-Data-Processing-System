package com.semitrack.rmi;

import com.semitrack.aggregator.AggregationStore;
import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.WaferTestResult;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * RMI server-side implementation of {@link TestResultService}.
 *
 * <p>Extends {@link UnicastRemoteObject} so that the instance is automatically
 * exported and stub/skeleton boilerplate is handled by the JVM.  All read
 * operations delegate to the shared {@link AggregationStore} which is itself
 * thread-safe (uses a ReadWriteLock internally).
 *
 * <p><b>Registration example:</b>
 * <pre>{@code
 * TestResultService stub = new TestResultServiceImpl(aggregatorService.getStore());
 * Registry registry = LocateRegistry.createRegistry(1099);
 * registry.rebind("TestResultService", stub);
 * }</pre>
 */
public class TestResultServiceImpl extends UnicastRemoteObject implements TestResultService {

    private static final long serialVersionUID = 1L;
    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    private final AggregationStore store;

    public TestResultServiceImpl(AggregationStore store) throws RemoteException {
        super();   // exports the object
        this.store = store;
    }

    @Override
    public List<WaferTestResult> getAllResults() throws RemoteException {
        LOG.info("[RMI] getAllResults() called");
        return store.allResults();
    }

    @Override
    public List<WaferTestResult> getResultsForWafer(String waferId) throws RemoteException {
        LOG.info("[RMI] getResultsForWafer(" + waferId + ") called");
        return store.resultsForWafer(waferId);
    }

    @Override
    public int getTotalResultCount() throws RemoteException {
        LOG.info("[RMI] getTotalResultCount() called");
        return store.totalResults();
    }
}
