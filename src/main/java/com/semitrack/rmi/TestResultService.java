package com.semitrack.rmi;

import com.semitrack.model.WaferTestResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI remote interface that exposes test results to remote clients.
 *
 * <p>Any object implementing this interface and exported via
 * {@link java.rmi.server.UnicastRemoteObject} can be looked up from a
 * separate JVM through the RMI registry, allowing distributed read access
 * to the aggregation store without sharing memory.
 *
 * <p>Every method declares {@link RemoteException} as required by the RMI spec.
 */
public interface TestResultService extends Remote {

    /**
     * Fetch all test results currently held by the aggregation store.
     *
     * @return a snapshot list; never null, may be empty
     * @throws RemoteException on network or serialisation failure
     */
    List<WaferTestResult> getAllResults() throws RemoteException;

    /**
     * Fetch results for a specific wafer.
     *
     * @param waferId the wafer identifier (e.g. "ST-01-W001")
     * @return results in die-coordinate order; empty if wafer not found
     * @throws RemoteException on network or serialisation failure
     */
    List<WaferTestResult> getResultsForWafer(String waferId) throws RemoteException;

    /**
     * Return the total number of test results in the store.
     *
     * @return count ≥ 0
     * @throws RemoteException on network or serialisation failure
     */
    int getTotalResultCount() throws RemoteException;
}
