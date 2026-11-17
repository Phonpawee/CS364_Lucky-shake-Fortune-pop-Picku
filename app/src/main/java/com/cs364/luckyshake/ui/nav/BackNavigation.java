package com.cs364.luckyshake.ui.nav;

import android.content.Intent;

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
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBack.run();
            }
        });
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
