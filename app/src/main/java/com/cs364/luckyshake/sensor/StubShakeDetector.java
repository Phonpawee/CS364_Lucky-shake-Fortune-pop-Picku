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
    private volatile OnShakeListener listener;
    private volatile boolean running;
    private volatile int sensitivity = GameConfig.DEFAULT_SENSITIVITY;
    private long lastShakeAt = Long.MIN_VALUE; // guarded by this

    /** Uses a monotonic clock, so changing the phone's time does not break the debounce. */
    public StubShakeDetector() {
        this(() -> System.nanoTime() / 1_000_000L);
    }

    public StubShakeDetector(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setOnShakeListener(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSensitivity(int sensitivity) {
        this.sensitivity = GameConfig.clampSensitivity(sensitivity);
    }

    public int getSensitivity() {
        return sensitivity;
    }

    /**
     * Fires the listener if running and the debounce time has passed. The check and the
     * timestamp update happen together, so two calls at the same moment fire only once.
     * The listener runs on the calling thread; call this from the main thread.
     */
    @Override
    public void simulateShake() {
        OnShakeListener target = listener;
        if (!running || target == null) {
            return;
        }
        synchronized (this) {
            long now = clock.nowMillis();
            if (lastShakeAt != Long.MIN_VALUE && now - lastShakeAt < GameConfig.SHAKE_DEBOUNCE_MS) {
                return;
            }
            lastShakeAt = now;
        }
        target.onShake();
    }
}