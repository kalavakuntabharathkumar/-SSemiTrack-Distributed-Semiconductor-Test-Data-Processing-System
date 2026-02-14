package com.semitrack.logger;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton application logger for SemiTrack.
 *
 * Uses the "initialization-on-demand holder" idiom (Bill Pugh Singleton) so
 * the instance is created lazily and thread-safely without synchronised blocks.
 *
 * All log methods are thread-safe because they delegate to
 * {@link System#out}/{@link System#err} which are synchronised in the JDK.
 */
public final class SemiTrackLogger {

    /** Prevents direct instantiation. */
    private SemiTrackLogger() {}

    // ── Initialization-on-demand holder (thread-safe, lazy) ─────────────────
    private static final class Holder {
        static final SemiTrackLogger INSTANCE = new SemiTrackLogger();
    }

    /** Returns the single shared logger instance. */
    public static SemiTrackLogger getInstance() {
        return Holder.INSTANCE;
    }

    // ── Logging methods ──────────────────────────────────────────────────────

    public void info(String message) {
        log("INFO ", message);
    }

    public void warn(String message) {
        log("WARN ", message);
    }

    public void error(String message) {
        System.err.printf("[%s] ERROR  %s%n", Instant.now(), message);
    }

    public void debug(String message) {
        log("DEBUG", message);
    }

    private void log(String level, String message) {
        System.out.printf("[%s] %s  %s%n", Instant.now(), level, message);
    }
}
