package com.semitrack.config;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton application configuration store.
 *
 * Centralises runtime tunables (wafer grid size, failure threshold, thread
 * pool size, etc.) so that all components read from one source of truth
 * without needing to pass configuration objects through every constructor.
 *
 * Implemented with the "initialization-on-demand holder" idiom for thread
 * safety without locking overhead on every access.
 */
public final class AppConfig {

    // ── Singleton holder ─────────────────────────────────────────────────────

    private AppConfig() {}

    private static final class Holder {
        static final AppConfig INSTANCE = new AppConfig();
    }

    public static AppConfig getInstance() {
        return Holder.INSTANCE;
    }

    // ── Configuration properties ─────────────────────────────────────────────

    /** Maximum X coordinate of the wafer die grid (inclusive). */
    private volatile int waferGridMaxX = 24;

    /** Maximum Y coordinate of the wafer die grid (inclusive). */
    private volatile int waferGridMaxY = 24;

    /**
     * Fraction of FAILs in a batch that triggers an Observer notification.
     * Range [0.0, 1.0].
     */
    private volatile double batchFailureThreshold = 0.3;

    /** Number of dies generated per station run per wafer. */
    private volatile int diesPerWafer = 30;

    /** Number of test-result wafers generated per station. */
    private volatile int wafersPerStation = 3;

    /** Port on which the gRPC server listens. */
    private volatile int grpcPort = 9090;

    /** Port on which the RMI registry is bound. */
    private volatile int rmiPort = 1099;

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getWaferGridMaxX()           { return waferGridMaxX; }
    public int getWaferGridMaxY()           { return waferGridMaxY; }
    public double getBatchFailureThreshold(){ return batchFailureThreshold; }
    public int getDiesPerWafer()            { return diesPerWafer; }
    public int getWafersPerStation()        { return wafersPerStation; }
    public int getGrpcPort()               { return grpcPort; }
    public int getRmiPort()                { return rmiPort; }

    // ── Setters (runtime override, e.g. from tests) ──────────────────────────

    public void setWaferGridMaxX(int v)           { this.waferGridMaxX = v; }
    public void setWaferGridMaxY(int v)           { this.waferGridMaxY = v; }
    public void setBatchFailureThreshold(double v){ this.batchFailureThreshold = v; }
    public void setDiesPerWafer(int v)            { this.diesPerWafer = v; }
    public void setWafersPerStation(int v)        { this.wafersPerStation = v; }
    public void setGrpcPort(int v)               { this.grpcPort = v; }
    public void setRmiPort(int v)                { this.rmiPort = v; }
}
