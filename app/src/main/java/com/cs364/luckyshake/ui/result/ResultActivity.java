package com.cs364.luckyshake.ui.result;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.R;
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

    private View stick;
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
        home.setOnClickListener(view -> finish());
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
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
                ObjectAnimator.ofFloat(stick, View.ROTATION, -28f, 0f));
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
            stick.setRotation(0f);
        }
        super.onPause();
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
