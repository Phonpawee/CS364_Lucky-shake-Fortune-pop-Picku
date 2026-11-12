package com.cs364.luckyshake.sensor;

/**
 * Detects a shake of the phone. One shake calls the listener once.
 * Do not change these signatures without telling the team (see README).
 *
 * Usage in an Activity/Fragment:
 * <pre>
 * detector = ShakeDetector.create();
 * detector.setOnShakeListener(() -> draw());
 * // onResume: detector.start();   onPause: detector.stop();
 * </pre>
 */
public interface ShakeDetector {

    /** Called on the main thread when a shake is detected. */
    interface OnShakeListener {
        void onShake();
    }

    /** Starts listening (call in onResume). Calling it twice is safe. */
    void start();

    /** Stops listening and releases the sensor (call in onPause). Calling it twice is safe. */
    void stop();

    /** True between start() and stop(). */
    boolean isRunning();

    /** Sets the listener, or null to remove it. */
    void setOnShakeListener(OnShakeListener listener);

    /**
     * Sets how easily a shake is detected, GameConfig.MIN_SENSITIVITY..MAX_SENSITIVITY
     * (higher = lighter shake is enough). Values outside the range are clamped.
     */
    void setSensitivity(int sensitivity);

    /**
     * Acts as if the phone was shaken (for tests, the emulator and a debug button).
     * Fires the listener only while running, and follows the same debounce as a real shake.
     */
    void simulateShake();

    /**
     * The detector the app should use. Returns the stub until issue #2 is merged;
     * the real one will need a Context, so this method will then take one.
     */
    static ShakeDetector create() {
        return new StubShakeDetector();
    }
}