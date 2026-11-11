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
    private OnShakeListener listener;
    private boolean running;
    private int sensitivity = GameConfig.DEFAULT_SENSITIVITY;
    private long lastShakeAt = Long.MIN_VALUE;

    public StubShakeDetector() {
        this(System::currentTimeMillis);
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

    @Override
    public void simulateShake() {
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