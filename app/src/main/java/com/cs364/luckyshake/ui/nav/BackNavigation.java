package com.cs364.luckyshake.ui.nav;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cs364.luckyshake.MainActivity;
import com.cs364.luckyshake.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class BackNavigation {
    private BackNavigation() {
    }

    public static void register(AppCompatActivity activity, Runnable onBack) {
        if (!(activity instanceof MainActivity)) attachBackButton(activity);
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBack.run();
            }
        });
    }

    /** Shared visible Back action uses exactly the same dispatcher as system Back. */
    public static void attachBackButton(AppCompatActivity activity) {
        if (activity.findViewById(R.id.app_back_button) != null) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        View screen = content.getChildAt(0);
        if (screen == null) return;
        content.removeView(screen);
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(androidx.core.content.ContextCompat.getColor(
                activity, R.color.home_paper));
        View bar = activity.getLayoutInflater().inflate(R.layout.view_back_bar, wrapper, false);
        wrapper.addView(bar);
        wrapper.addView(screen, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content.addView(wrapper, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.findViewById(R.id.app_back_button).setOnClickListener(v ->
                activity.getOnBackPressedDispatcher().onBackPressed());
        ViewCompat.setOnApplyWindowInsetsListener(wrapper, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(0, Math.max(bars.top, cutout.top), 0, 0);
            // The wrapper owns the top inset; existing screens still own side/bottom insets.
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.systemBars(),
                            Insets.of(bars.left, 0, bars.right, bars.bottom))
                    .setInsets(WindowInsetsCompat.Type.displayCutout(),
                            Insets.of(cutout.left, 0, cutout.right, cutout.bottom))
                    .build();
        });
        ViewCompat.requestApplyInsets(wrapper);
    }

    public static AlertDialog confirmDiscard(AppCompatActivity activity, @StringRes int title,
                                              @StringRes int message, Runnable discard) {
        return new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.back_stay, null)
                .setPositiveButton(R.string.menu_form_btn_discard, (dialog, which) -> discard.run())
                .show();
    }

    public static void goHome(AppCompatActivity activity) {
        activity.startActivity(new Intent(activity, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        activity.finish();
    }
}
