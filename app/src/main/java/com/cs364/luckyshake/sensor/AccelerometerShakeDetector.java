package com.cs364.luckyshake.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.cs364.luckyshake.logic.GameConfig;

/**
 * Real ShakeDetector that reads Sensor.TYPE_ACCELEROMETER through SensorManager
 * (no third-party library).
 *
 * <p>For each reading it computes g = sqrt(x² + y² + z²) − GRAVITY_EARTH, which is about 0
 * when the phone lies still. A reading with g above the threshold counts as a shake, where
 * threshold = SHAKE_THRESHOLD_BASE − SHAKE_THRESHOLD_STEP × sensitivity. One physical shake
 * gives many readings above the threshold, so the listener fires at most once per
 * GameConfig.SHAKE_DEBOUNCE_MS. {@link #simulateShake()} uses the same debounce.
 *
 * <p>If the phone has no accelerometer the app does not crash: start() and stop() still
 * work, only {@link #simulateShake()} fires, and {@link #hasSensor()} returns false so a
 * screen can show a button instead.
 *
 * <p>Logcat tag {@value #TAG} logs each register/unregister, to check that leaving the app
 * releases the sensor.
 */
public class AccelerometerShakeDetector implements ShakeDetector, SensorEventListener {
    static final String TAG = "ShakeDetector";

    /** Source of the current time; tests can replace it. */
    public interface Clock {
        long nowMillis();
    }

    /** Hides SensorManager so unit tests can run without a device. */
    interface SensorSource {
        boolean isAvailable();

        /** Returns false if the listener could not be registered. */
        boolean register(SensorEventListener listener);

        void unregister(SensorEventListener listener);
    }

    private final SensorSource source;
    private final Clock clock;
    // All state below is guarded by "this".
    private OnShakeListener listener;
    private boolean running;
    private int sensitivity = GameConfig.DEFAULT_SENSITIVITY;
    private double threshold = thresholdFor(GameConfig.DEFAULT_SENSITIVITY);
    private long lastShakeAt = Long.MIN_VALUE;

    /** Uses a monotonic clock, so changing the phone's time does not break the debounce. */
    public AccelerometerShakeDetector(Context context) {
        this(new AndroidSensorSource(context), () -> System.nanoTime() / 1_000_000L);
    }

    AccelerometerShakeDetector(SensorSource source, Clock clock) {
        this.source = source;
        this.clock = clock;
    }

    /** Shake threshold in m/s² for a sensitivity; out-of-range values are clamped first. */
    static double thresholdFor(int sensitivity) {
        return GameConfig.SHAKE_THRESHOLD_BASE
                - GameConfig.SHAKE_THRESHOLD_STEP * GameConfig.clampSensitivity(sensitivity);
    }

    /** Total acceleration minus gravity, in m/s². */
    static double accelerationWithoutGravity(float x, float y, float z) {
        return Math.sqrt((double) x * x + (double) y * y + (double) z * z)
                - SensorManager.GRAVITY_EARTH;
    }

    /** False if the phone has no accelerometer; then only simulateShake() can fire. */
    public boolean hasSensor() {
        return source.isAvailable();
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        source.register(this);
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        source.unregister(this);
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
        this.threshold = thresholdFor(this.sensitivity);
    }

    public synchronized int getSensitivity() {
        return sensitivity;
    }

    /** Current threshold in m/s². */
    public synchronized double getThreshold() {
        return threshold;
    }

    @Override
    public synchronized void simulateShake() {
        fireIfNotDebounced();
    }

    /** Called on the main thread (registerListener is given no Handler). */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values == null || event.values.length < 3) {
            return;
        }
        onAcceleration(event.values[0], event.values[1], event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy is not used.
    }

    /** One accelerometer reading in m/s². Package-private so tests can feed values. */
    synchronized void onAcceleration(float x, float y, float z) {
        // A reading that was queued before stop() can still arrive; ignore it.
        if (!running) {
            return;
        }
        if (accelerationWithoutGravity(x, y, z) > threshold) {
            fireIfNotDebounced();
        }
    }

    /**
     * Shared by real and simulated shakes, so both follow one debounce. Holds the lock
     * while calling the listener, like StubShakeDetector; the listener may call stop()
     * because the lock is reentrant.
     */
    private void fireIfNotDebounced() {
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

    /** The real SensorManager. A missing service or sensor is logged, not thrown. */
    private static final class AndroidSensorSource implements SensorSource {
        private final SensorManager manager;
        private final Sensor sensor;

        AndroidSensorSource(Context context) {
            manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensor = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensor == null) {
                Log.w(TAG, "No accelerometer on this device; only simulateShake() will fire");
            }
        }

        @Override
        public boolean isAvailable() {
            return sensor != null;
        }

        @Override
        public boolean register(SensorEventListener listener) {
            if (sensor == null) {
                return false;
            }
            boolean ok = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
            if (ok) {
                Log.d(TAG, "Accelerometer listener registered");
            } else {
                Log.w(TAG, "registerListener failed; only simulateShake() will fire");
            }
            return ok;
        }

        @Override
        public void unregister(SensorEventListener listener) {
            if (sensor == null) {
                return;
            }
            manager.unregisterListener(listener, sensor);
            Log.d(TAG, "Accelerometer listener unregistered");
        }
    }
}
