package com.cs364.luckyshake.sensor;

import com.cs364.luckyshake.logic.GameConfig;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StubShakeDetectorTest {
    private long now;
    private int shakes;
    private StubShakeDetector detector;

    @Before
    public void setUp() {
        now = 10_000L;
        shakes = 0;
        detector = new StubShakeDetector(() -> now);
        detector.setOnShakeListener(() -> shakes++);
    }

    @Test
    public void simulateShake_whenStopped_doesNothing() {
        detector.simulateShake();
        assertEquals(0, shakes);
    }

    @Test
    public void simulateShake_whenRunning_firesOnce() {
        detector.start();
        assertTrue(detector.isRunning());
        detector.simulateShake();
        assertEquals(1, shakes);
    }

    @Test
    public void simulateShake_respectsDebounce() {
        detector.start();
        detector.simulateShake();
        now += GameConfig.SHAKE_DEBOUNCE_MS - 1;
        detector.simulateShake();
        assertEquals(1, shakes);
        now += 1;
        detector.simulateShake();
        assertEquals(2, shakes);
    }

    @Test
    public void stop_stopsFiring() {
        detector.start();
        detector.stop();
        assertFalse(detector.isRunning());
        detector.simulateShake();
        assertEquals(0, shakes);
    }

    @Test
    public void setSensitivity_isClamped() {
        detector.setSensitivity(0);
        assertEquals(GameConfig.MIN_SENSITIVITY, detector.getSensitivity());
        detector.setSensitivity(99);
        assertEquals(GameConfig.MAX_SENSITIVITY, detector.getSensitivity());
    }
}