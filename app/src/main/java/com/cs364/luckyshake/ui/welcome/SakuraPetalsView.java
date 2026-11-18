package com.cs364.luckyshake.ui.welcome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;

/** Small decorative particle layer. Never receives touches or accessibility focus. */
public final class SakuraPetalsView extends View {
    private static final int COUNT = 22;
    private final float[] x = new float[COUNT], phase = new float[COUNT], size = new float[COUNT];
    private final float[] drift = new float[COUNT], spin = new float[COUNT];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path petal = new Path();
    private final boolean night;
    private ValueAnimator animator;
    private boolean active;
    private float progress;

    public SakuraPetalsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        Random random = new Random(17);
        for (int i = 0; i < COUNT; i++) {
            x[i] = random.nextFloat();
            phase[i] = random.nextFloat();
            size[i] = 5f + random.nextFloat() * 7f;
            drift[i] = random.nextFloat() * 6.28f;
            spin[i] = 90f + random.nextFloat() * 270f;
        }
        // Two soft lobes and a small notch, like a single cherry blossom petal.
        petal.moveTo(0, 0.9f);
        petal.cubicTo(-1.1f, 0.1f, -0.7f, -1f, -0.14f, -0.64f);
        petal.lineTo(0, -0.45f);
        petal.lineTo(0.14f, -0.64f);
        petal.cubicTo(0.8f, -1f, 1f, 0.2f, 0, 0.9f);
        petal.close();
    }

    public void setActive(boolean value) {
        active = value;
        updateAnimation();
    }

    private void updateAnimation() {
        boolean run = active && isAttachedToWindow() && isShown()
                && getWindowVisibility() == VISIBLE && ValueAnimator.areAnimatorsEnabled();
        if (run && animator == null) {
            animator = ValueAnimator.ofFloat(progress, progress + 1f);
            animator.setDuration(24000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(value -> {
                progress = (float) value.getAnimatedValue() % 1f;
                invalidate();
            });
            animator.start();
        } else if (!run && animator != null) {
            animator.removeAllUpdateListeners();
            animator.cancel();
            animator = null;
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimation();
    }
    @Override protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.removeAllUpdateListeners();
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }
    @Override public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        updateAnimation();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateAnimation();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float margin = 24f * density;
        for (int i = 0; i < COUNT; i++) {
            float t = (progress + phase[i]) % 1f;
            float px = x[i] * getWidth() + (float) Math.sin(t * 6.28f + drift[i]) * 24f * density
                    + (t - 0.5f) * 65f * density;
            float py = t * (getHeight() + margin * 2) - margin;
            float radius = size[i] * density;
            // Alternating size, opacity and gentle flips provide depth without obscuring the art.
            paint.setColor(night ? (i % 3 == 0 ? 0xFFEEE6FF : 0xFFF6BED4)
                    : (i % 3 == 0 ? 0xFFFFF0E9 : 0xFFF5ABC0));
            paint.setAlpha(i % 3 == 0 ? 190 : 130);
            canvas.save();
            canvas.translate(px, py);
            canvas.rotate(t * spin[i] + drift[i] * 57f);
            canvas.scale(radius * (0.45f + 0.55f * Math.abs((float) Math.cos(t * 6.28f + drift[i]))), radius);
            canvas.drawPath(petal, paint);
            canvas.restore();
        }
    }
}
