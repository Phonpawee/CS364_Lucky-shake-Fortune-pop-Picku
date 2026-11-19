package com.cs364.luckyshake.ui.result;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ResultCardImageTest {
    @Test
    public void pngContainsFullCardBeyondVisibleViewport() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            for (String text : new String[]{"Stick 7", "Fried rice", "Luck ★★★", "Your fortune"}) {
                TextView label = new TextView(context);
                label.setText(text);
                card.addView(label, new LinearLayout.LayoutParams(400, 200));
            }
            View bottom = new View(context);
            bottom.setBackgroundColor(Color.RED);
            card.addView(bottom, new LinearLayout.LayoutParams(400, 100));
            card.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY));
            card.layout(0, 0, 400, 900);
            File png = null;
            Bitmap decoded = null;
            try {
                png = File.createTempFile("card-test", ".png", context.getCacheDir());
                ResultCardImage.capture(card, png);
                decoded = BitmapFactory.decodeFile(png.getPath());
                assertNotNull(decoded);
                assertEquals(400, decoded.getWidth());
                assertEquals(900, decoded.getHeight());
                assertEquals(Color.RED, decoded.getPixel(200, 850));
            } catch (IOException error) {
                throw new AssertionError(error);
            } finally {
                if (decoded != null) decoded.recycle();
                if (png != null) png.delete();
            }
        });
    }
}
