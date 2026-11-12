package com.cs364.luckyshake.sensor;

import com.cs364.luckyshake.logic.GameConfig;

/**
 * Temporary ShakeDetector so screens can be wired before issue #2 is done.
 * It never reads the sensor; only {@link #simulateShake()} fires the listener.
 */
public class StubShakeDetector implements ShakeDetector {
    /** Source of the current time; tests can replace it. */
    public interface Clock {
        long nowMillis();
    }

    private final Clock clock;
    // All state below is guarded by "this".
    private OnShakeListener listener;
    private boolean running;
    private int sensitivity = GameConfig.DEFAULT_SENSITIVITY;
    private long lastShakeAt = Long.MIN_VALUE;

    /** Uses a monotonic clock, so changing the phone's time does not break the debounce. */
    public StubShakeDetector() {
        this(() -> System.nanoTime() / 1_000_000L);
    }

    public StubShakeDetector(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized void start() {
        running = true;
    }

    @Override
    public synchronized void stop() {
        running = false;
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    @Override
    public synchronized void setOnShakeListener(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void setSensitivity(int sensitivity) {
        this.sensitivity = GameConfig.clampSensitivity(sensitivity);
    }

    public synchronized int getSensitivity() {
        return sensitivity;
    }

    /**
     * Fires the listener if running and the debounce time has passed.
     * The whole call holds the lock, so stop() or setOnShakeListener(null) from another
     * thread cannot slip in between the check and the callback, and two calls at the same
     * moment fire only once. The listener runs on the calling thread; call this from the
     * main thread. The listener may call stop() or other methods here (the lock is reentrant).
     */
    @Override
    public synchronized void simulateShake() {
        if (!running || listener == null) {
            return;
        }
        long now = clock.nowMillis();
        if (lastShakeAt != Long.MIN_VALUE && now - lastShakeAt < GameConfig.SHAKE_DEBOUNCE_MS) {
            return;
        }
        lastShakeAt = now;
        listener.onShake();
    }
}