package com.cs364.luckyshake.ui.home;

import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.cs364.luckyshake.R;

/** Reflows controls using their actual column width after text size has been applied. */
public final class ResponsiveLayout {
    private ResponsiveLayout() {
    }

    public static void applyHome(AppCompatActivity activity) {
        reflow(activity.findViewById(R.id.home_picker_row), 330);
        reflow(activity.findViewById(R.id.home_filter_footer), 330);
    }

    public static void applyResult(AppCompatActivity activity) {
        reflow(activity.findViewById(R.id.result_action_row), 330);
    }

    private static void reflow(LinearLayout row, int minimumWidthDp) {
        row.post(() -> {
            float density = row.getResources().getDisplayMetrics().density;
            float fontScale = row.getResources().getConfiguration().fontScale;
            float widthDp = row.getWidth() / density;
            boolean stacked = widthDp < minimumWidthDp || fontScale >= 1.5f;
            row.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            for (int i = 0; i < row.getChildCount(); i++) {
                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) row.getChildAt(i).getLayoutParams();
                params.width = stacked ? LinearLayout.LayoutParams.MATCH_PARENT : 0;
                params.weight = stacked ? 0f : 1f;
                params.setMarginStart(!stacked && i > 0 ? Math.round(6 * density) : 0);
                params.setMarginEnd(0);
                row.getChildAt(i).setLayoutParams(params);
            }
        });
    }
}
