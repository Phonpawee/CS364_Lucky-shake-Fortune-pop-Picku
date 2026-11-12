package com.cs364.luckyshake.sensor;

import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.cs364.luckyshake.logic.GameConfig;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccelerometerShakeDetectorTest {
    private static final double EPS = 1e-9;
    private static final float G = SensorManager.GRAVITY_EARTH;

    /** Counts register/unregister calls instead of touching a real SensorManager. */
    private static class FakeSource implements AccelerometerShakeDetector.SensorSource {
        boolean available = true;
        int registers;
        int unregisters;

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public boolean register(SensorEventListener listener) {
            registers++;
            return available;
        }

        @Override
        public void unregister(SensorEventListener listener) {
            unregisters++;
        }
    }

    private long now;
    private int shakes;
    private FakeSource source;
    private AccelerometerShakeDetector detector;

    @Before
    public void setUp() {
        now = 10_000L;
        shakes = 0;
        source = new FakeSource();
        detector = new AccelerometerShakeDetector(source, () -> now);
        detector.setOnShakeListener(() -> shakes++);
    }

    /** A reading straight up the z axis whose g (minus gravity) is exactly extra m/s². */
    private void feed(double extra) {
        detector.onAcceleration(0f, 0f, (float) (G + extra));
    }

    // ------------------------------------------------------------ threshold

    @Test
    public void thresholdFor_followsFormula() {
        for (int s = GameConfig.MIN_SENSITIVITY; s <= GameConfig.MAX_SENSITIVITY; s++) {
            double expected = GameConfig.SHAKE_THRESHOLD_BASE - GameConfig.SHAKE_THRESHOLD_STEP * s;
            assertEquals(expected, AccelerometerShakeDetector.thresholdFor(s), EPS);
        }
    }

    @Test
    public void thresholdFor_higherSensitivityIsLower() {
        assertTrue(AccelerometerShakeDetector.thresholdFor(GameConfig.MAX_SENSITIVITY)
                < AccelerometerShakeDetector.thresholdFor(GameConfig.MIN_SENSITIVITY));
    }

    @Test
    public void thresholdFor_clampsOutOfRange() {
        assertEquals(AccelerometerShakeDetector.thresholdFor(GameConfig.MIN_SENSITIVITY),
                AccelerometerShakeDetector.thresholdFor(-3), EPS);
        assertEquals(AccelerometerShakeDetector.thresholdFor(GameConfig.MAX_SENSITIVITY),
                AccelerometerShakeDetector.thresholdFor(99), EPS);
    }

    @Test
    public void defaultSensitivity_isFive() {
        assertEquals(5, detector.getSensitivity());
        assertEquals(22.0 - 1.6 * 5, detector.getThreshold(), EPS);
    }

    @Test
    public void setSensitivity_isClampedAndUpdatesThreshold() {
        detector.setSensitivity(0);
        assertEquals(GameConfig.MIN_SENSITIVITY, detector.getSensitivity());
        assertEquals(AccelerometerShakeDetector.thresholdFor(GameConfig.MIN_SENSITIVITY),
                detector.getThreshold(), EPS);
        detector.setSensitivity(99);
        assertEquals(GameConfig.MAX_SENSITIVITY, detector.getSensitivity());
        assertEquals(AccelerometerShakeDetector.thresholdFor(GameConfig.MAX_SENSITIVITY),
                detector.getThreshold(), EPS);
    }

    @Test
    public void accelerationWithoutGravity_isZeroAtRest() {
        assertEquals(0.0, AccelerometerShakeDetector.accelerationWithoutGravity(0f, 0f, G), 1e-5);
        assertEquals(5.0, AccelerometerShakeDetector.accelerationWithoutGravity(3f, 4f, 0f) + G, 1e-5);
    }

    // ------------------------------------------------------- real readings

    @Test
    public void reading_aboveThreshold_fires() {
        detector.start();
        feed(detector.getThreshold() + 0.5);
        assertEquals(1, shakes);
    }

    @Test
    public void reading_atOrBelowThreshold_doesNotFire() {
        detector.start();
        feed(0);
        feed(detector.getThreshold() - 0.5);
        assertEquals(0, shakes);
    }

    @Test
    public void sensitivity_changesWhatCountsAsShake() {
        detector.start();
        double light = 6.0; // above threshold at 10 (6.0), below at 1 (20.4)
        detector.setSensitivity(GameConfig.MIN_SENSITIVITY);
        feed(light);
        assertEquals(0, shakes);
        detector.setSensitivity(GameConfig.MAX_SENSITIVITY);
        feed(light + 0.1);
        assertEquals(1, shakes);
    }

    @Test
    public void oneShake_manyReadings_firesOnce() {
        detector.start();
        double strong = detector.getThreshold() + 3;
        for (int i = 0; i < 25; i++) { // ~0.5 s of readings at 50 Hz
            feed(strong);
            now += 20;
        }
        assertEquals(1, shakes);
    }

    @Test
    public void readings_respectDebounce() {
        detector.start();
        double strong = detector.getThreshold() + 3;
        feed(strong);
        now += GameConfig.SHAKE_DEBOUNCE_MS - 1;
        feed(strong);
        assertEquals(1, shakes);
        now += 1;
        feed(strong);
        assertEquals(2, shakes);
    }

    @Test
    public void reading_whenStopped_doesNothing() {
        feed(detector.getThreshold() + 3);
        detector.start();
        detector.stop();
        feed(detector.getThreshold() + 3);
        assertEquals(0, shakes);
    }

    // ---------------------------------------------------------- simulate

    @Test
    public void simulateShake_whenRunning_firesOnce() {
        detector.start();
        detector.simulateShake();
        assertEquals(1, shakes);
    }

    @Test
    public void simulateShake_whenStopped_doesNothing() {
        detector.simulateShake();
        assertEquals(0, shakes);
    }

    @Test
    public void simulateAndRealShake_shareDebounce() {
        detector.start();
        detector.simulateShake();
        now += GameConfig.SHAKE_DEBOUNCE_MS - 1;
        feed(detector.getThreshold() + 3);
        assertEquals(1, shakes);
        now += 1;
        feed(detector.getThreshold() + 3);
        now += GameConfig.SHAKE_DEBOUNCE_MS - 1;
        detector.simulateShake();
        assertEquals(2, shakes);
    }

    @Test
    public void setOnShakeListener_null_stopsFiring() {
        detector.start();
        detector.setOnShakeListener(null);
        detector.simulateShake();
        feed(detector.getThreshold() + 3);
        assertEquals(0, shakes);
    }

    @Test
    public void listener_canCallStopInsideCallback() {
        detector.setOnShakeListener(() -> {
            shakes++;
            detector.stop();
        });
        detector.start();
        feed(detector.getThreshold() + 3);
        assertEquals(1, shakes);
        assertFalse(detector.isRunning());
        assertEquals(1, source.unregisters);
    }

    // --------------------------------------------------------- lifecycle

    @Test
    public void startAndStop_registerAndUnregisterOnce_evenWhenCalledTwice() {
        detector.start();
        detector.start();
        assertTrue(detector.isRunning());
        assertEquals(1, source.registers);
        detector.stop();
        detector.stop();
        assertFalse(detector.isRunning());
        assertEquals(1, source.unregisters);
    }

    @Test
    public void stop_beforeStart_doesNotUnregister() {
        detector.stop();
        assertEquals(0, source.unregisters);
    }

    @Test
    public void resumePauseCycles_balanceRegistrations() {
        for (int i = 0; i < 3; i++) {
            detector.start(); // onResume
            detector.stop();  // onPause
        }
        assertEquals(3, source.registers);
        assertEquals(3, source.unregisters);
    }

    @Test
    public void noAccelerometer_doesNotCrash_andSimulateStillWorks() {
        source.available = false;
        assertFalse(detector.hasSensor());
        detector.start();
        assertTrue(detector.isRunning());
        detector.simulateShake();
        assertEquals(1, shakes);
        detector.stop();
        assertFalse(detector.isRunning());
    }
}
