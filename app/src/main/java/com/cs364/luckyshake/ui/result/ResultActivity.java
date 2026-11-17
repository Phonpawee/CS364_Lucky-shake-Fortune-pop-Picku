package com.cs364.luckyshake.ui.result;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.net.Uri;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.ui.nav.BackNavigation;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.fortune.FortuneProvider;
import com.cs364.luckyshake.logic.DrawStatus;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.home.DrawState;
import com.cs364.luckyshake.ui.home.HomeViewModel;
import com.cs364.luckyshake.ui.menu.MenuLabels;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;

import java.util.Collections;

public class ResultActivity extends AppCompatActivity {
    public static final String EXTRA_MENU_ID = "com.cs364.luckyshake.extra.MENU_ID";
    private static final String STATE_MENU_ID = "result_menu_id";

    private static final String STATE_STICK_LANDED = "stick_landed";

    private static final float STICK_REST_ROTATION = 12f;

    private static final String STATE_EXPORT = "result_export_snapshot";
    private String pendingSnapshot;
    private View saveImage;
    private View resultCard;
    // Register unconditionally so a picker result can be delivered after recreation.
    private final ActivityResultLauncher<String> createImage = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("image/png"), this::saveImageDocument);

    private ImageView stick;
    private AnimatorSet stickFall;
    private boolean stickLanded;
    private FortuneProvider fortuneProvider;
    private long menuId;
    private int redrawLimit;
    private TextView quota;
    private TextView status;
    private View redraw;
    private View accept;
    private View home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pendingSnapshot = savedInstanceState.getString(STATE_EXPORT);
        }
        HomeViewModel viewModel = HomeViewModel.get(this);
        menuId = savedInstanceState == null
                ? getIntent().getLongExtra(EXTRA_MENU_ID, 0L)
                : savedInstanceState.getLong(STATE_MENU_ID);
        Menu menu = viewModel.currentState().menu;
        if (menu == null || menu.id != menuId) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        View root = findViewById(R.id.result_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        FontSizeHelper.apply(this, root);

        stick = findViewById(R.id.result_image_stick);
        stick.setColorFilter(new ColorMatrixColorFilter(new float[] {
                1.07f, 0f, 0f, 0f, 8f,
                0f, 1.07f, 0f, 0f, 8f,
                0f, 0f, 1.07f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
        }));
        stick.setRotation(STICK_REST_ROTATION);
        stickLanded = savedInstanceState != null && savedInstanceState.getBoolean(STATE_STICK_LANDED);
        fortuneProvider = new FortuneProvider(this);
        redrawLimit = GameConfig.clampRedrawLimit(getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                .getInt(PrefKeys.REROLL_QUOTA, GameConfig.MAX_REDRAWS));
        quota = findViewById(R.id.result_label_quota);
        status = findViewById(R.id.result_label_status);
        redraw = findViewById(R.id.result_btn_redraw);
        accept = findViewById(R.id.result_btn_accept);
        home = findViewById(R.id.result_btn_home);
        redraw.setOnClickListener(view -> viewModel.redraw());
        accept.setOnClickListener(view -> viewModel.accept());
        home.setOnClickListener(view -> BackNavigation.goHome(this));
        resultCard = findViewById(R.id.result_card);
        if (resultCard == null) {
            // Also supports tablet variants added by #16 before they gain export IDs.
            resultCard = (View) findViewById(R.id.result_label_stick).getParent();
        }
        saveImage = findViewById(R.id.result_btn_save_image);
        if (saveImage == null) {
            android.widget.LinearLayout actions = (android.widget.LinearLayout) home.getParent();
            View actionCard = (View) actions.getParent();
            android.widget.LinearLayout column = (android.widget.LinearLayout) actionCard.getParent();
            com.google.android.material.button.MaterialButton button =
                    new com.google.android.material.button.MaterialButton(this);
            button.setId(R.id.result_btn_save_image);
            button.setText(R.string.result_save_image);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            column.addView(button, column.indexOfChild(actionCard) + 1, params);
            saveImage = button;
        }
        saveImage.setEnabled(pendingSnapshot == null);
        saveImage.setOnClickListener(view -> chooseImageDestination());
        BackNavigation.register(this, () -> {
            if (viewModel.currentState().hasPendingResult()) {
                BackNavigation.confirmDiscard(this, R.string.back_draw_title,
                        R.string.back_draw_message, () -> {
                            BackNavigation.goHome(this);
                            viewModel.discardPendingResult();
                        });
            } else {
                BackNavigation.goHome(this);
            }
        });
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(STATE_EXPORT, pendingSnapshot);
        state.putLong(STATE_MENU_ID, menuId);
        state.putBoolean(STATE_STICK_LANDED, stickLanded);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stick == null || stickLanded) {
            return;
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            stickLanded = true;
            return;
        }

        float distance = 64f * getResources().getDisplayMetrics().density;
        stickFall = new AnimatorSet();
        stickFall.playTogether(
                ObjectAnimator.ofFloat(stick, View.TRANSLATION_Y, -distance, 0f),
                ObjectAnimator.ofFloat(stick, View.ROTATION, -28f, STICK_REST_ROTATION));
        stickFall.setDuration(480);
        stickFall.setInterpolator(new AccelerateInterpolator());
        stickFall.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                stickLanded = true;
                stickFall = null;
            }
        });
        stickFall.start();
    }

    @Override
    protected void onPause() {
        if (stickFall != null) {
            stickFall.removeAllListeners();
            stickFall.cancel();
            stickFall = null;
            stick.setTranslationY(0f);
            stick.setRotation(STICK_REST_ROTATION);
        }
        super.onPause();
    }

    private void chooseImageDestination() {
        if (pendingSnapshot != null) return;
        File snapshot = null;
        try {
            snapshot = File.createTempFile("fortune-", ".png", getCacheDir());
            ResultCardImage.capture(resultCard, snapshot);
            pendingSnapshot = snapshot.getName();
            saveImage.setEnabled(false);
            createImage.launch("LuckyShake-" + System.currentTimeMillis() + ".png");
        } catch (IOException | RuntimeException | OutOfMemoryError error) {
            if (snapshot != null) snapshot.delete();
            pendingSnapshot = null;
            saveImage.setEnabled(true);
            Toast.makeText(this, R.string.result_save_error, Toast.LENGTH_LONG).show();
        }
    }

    private void saveImageDocument(Uri uri) {
        String snapshotName = pendingSnapshot;
        pendingSnapshot = null;
        if (saveImage != null) saveImage.setEnabled(true);
        if (snapshotName == null) {
            Toast.makeText(this, uri == null ? R.string.result_save_cancelled
                    : R.string.result_save_error, Toast.LENGTH_LONG).show();
            return;
        }
        File snapshot = new File(getCacheDir(), snapshotName);
        if (uri == null) {
            snapshot.delete();
            Toast.makeText(this, R.string.result_save_cancelled, Toast.LENGTH_SHORT).show();
            return;
        }
        // Keep provider I/O off the UI thread, and use application context across rotation.
        Context context = getApplicationContext();
        new Thread(() -> {
            int message = R.string.result_save_success;
            try (FileInputStream input = new FileInputStream(snapshot);
                 OutputStream output = context.getContentResolver().openOutputStream(uri, "w")) {
                if (output == null) throw new IOException("Document provider returned no stream");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.flush();
            } catch (IOException | RuntimeException error) {
                message = R.string.result_save_error;
            } finally {
                snapshot.delete();
            }
            int result = message;
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show());
        }, "result-image-save").start();
    }

    private void render(DrawState state) {
        Menu menu = state.menu;
        if (menu == null) {
            finish();
            return;
        }

        menuId = menu.id;
        boolean thai = "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
        String menuName = menu.getName(thai);
        int stars = GameConfig.LEVEL_COUNT - state.level;
        String starSymbols = repeatSymbol(R.string.result_label_star, stars);
        setText(R.id.result_label_stick, getString(R.string.result_label_stick, state.stickNo));
        setText(R.id.result_label_menu, getString(R.string.result_label_menu, menuName));
        setText(R.id.result_label_type_tags, MenuLabels.typeAndTags(this, menu));
        setText(R.id.result_label_stars, getString(R.string.result_label_stars, starSymbols));
        // Keep the fortune variant stable when Android recreates the activity.
        int fortuneIndex = Math.floorMod(state.stickNo - 1, 3);
        setText(R.id.result_label_fortune,
                fortuneProvider.getFortune(stars, fortuneIndex, menuName));

        int remaining = state.redrawsLeft;
        String dots = repeatSymbol(R.string.result_label_dot_remaining, remaining)
                + repeatSymbol(R.string.result_label_dot_used, Math.max(0, redrawLimit - remaining));
        quota.setText(getString(R.string.result_label_quota, dots, remaining, redrawLimit));
        quota.setVisibility(state.accepted ? View.GONE : View.VISIBLE);
        redraw.setEnabled(state.canRedraw());
        redraw.setVisibility(state.accepted ? View.GONE : View.VISIBLE);
        accept.setVisibility(state.accepted ? View.GONE : View.VISIBLE);
        home.setVisibility(state.accepted ? View.VISIBLE : View.GONE);

        int message = statusMessage(state);
        status.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (message != 0) {
            status.setText(message);
        }
    }

    private int statusMessage(DrawState state) {
        if (state.accepted) return R.string.result_msg_accepted;
        if (state.status == DrawStatus.QUOTA_EMPTY || state.redrawsLeft == 0) {
            return R.string.result_msg_quota_empty;
        }
        if (state.status == DrawStatus.NO_MATCH) return R.string.home_msg_no_match;
        if (state.status == DrawStatus.SET_EMPTY) return R.string.home_msg_set_empty;
        if (state.status == DrawStatus.FEW_ITEMS) return R.string.home_msg_few_items;
        return 0;
    }

    private String repeatSymbol(int symbol, int count) {
        return String.join("", Collections.nCopies(count, getString(symbol)));
    }

    private void setText(int id, String value) {
        TextView label = findViewById(id);
        label.setText(value);
    }
}
