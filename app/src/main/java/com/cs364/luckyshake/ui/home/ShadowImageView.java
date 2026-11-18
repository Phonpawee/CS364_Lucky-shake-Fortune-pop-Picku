package com.cs364.luckyshake.ui.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** Draws a soft shadow from the artwork's alpha channel, including transparent edges. */
public final class ShadowImageView extends AppCompatImageView {
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final int[] shadowOffset = new int[2];
    private Bitmap shadow;
    private Drawable shadowSource;

    public ShadowImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        shadowPaint.setColor(0xC22A1A28);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        shadow = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable artwork = getDrawable();
        float density = getResources().getDisplayMetrics().density;
        if (artwork != null && getWidth() > 0 && getHeight() > 0) {
            if (shadow == null || shadowSource != artwork) {
                Bitmap mask = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                Canvas maskCanvas = new Canvas(mask);
                maskCanvas.translate(getPaddingLeft(), getPaddingTop());
                maskCanvas.concat(getImageMatrix());
                artwork.draw(maskCanvas);

                Paint blur = new Paint();
                blur.setMaskFilter(new BlurMaskFilter(5 * density, BlurMaskFilter.Blur.NORMAL));
                shadow = mask.extractAlpha(blur, shadowOffset);
                mask.recycle();
                shadowSource = artwork;
            }
            canvas.drawBitmap(shadow, shadowOffset[0] + density,
                    shadowOffset[1] + 3 * density, shadowPaint);
        }
        super.onDraw(canvas);
    }
}
