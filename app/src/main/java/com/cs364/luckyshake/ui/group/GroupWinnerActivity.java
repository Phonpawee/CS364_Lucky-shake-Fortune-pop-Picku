package com.cs364.luckyshake.ui.group;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.fortune.FortuneProvider;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.nav.BackNavigation;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;

import java.util.Collections;

/**
 * Screen displaying the winning option drawn from group decision mode (#39).
 * Renders stick, star level, fortune prediction, proposers list, and options to start a new round.
 */
public class GroupWinnerActivity extends AppCompatActivity {
    public static final String EXTRA_METHOD = "extra_method";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_VOTES = "extra_votes";
    public static final String EXTRA_PROPOSERS = "extra_proposers";
    public static final String EXTRA_DRAW_LEVEL = "extra_draw_level";
    public static final String EXTRA_STICK_NO = "extra_stick_no";

    private static final String STATE_STICK_LANDED = "stick_landed";
    private static final float STICK_REST_ROTATION = 12f;

    private ImageView stick;
    private AnimatorSet stickFall;
    private boolean stickLanded;
    private FortuneProvider fortuneProvider;

    public static Intent newIntent(Context context, ProposalMethod method, String label, int votes,
                                  int[] proposers, int drawLevel, int stickNo) {
        Intent intent = new Intent(context, GroupWinnerActivity.class);
        intent.putExtra(EXTRA_METHOD, method != null ? method.name() : ProposalMethod.FOOD_TYPE.name());
        intent.putExtra(EXTRA_LABEL, label != null ? label : "");
        intent.putExtra(EXTRA_VOTES, votes);
        intent.putExtra(EXTRA_PROPOSERS, proposers != null ? proposers : new int[0]);
        intent.putExtra(EXTRA_DRAW_LEVEL, drawLevel);
        intent.putExtra(EXTRA_STICK_NO, stickNo);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_winner);

        View root = findViewById(R.id.group_winner_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fortuneProvider = new FortuneProvider(this);

        String methodStr = getIntent().getStringExtra(EXTRA_METHOD);
        ProposalMethod method = ProposalMethod.FOOD_TYPE;
        if (methodStr != null) {
            try {
                method = ProposalMethod.valueOf(methodStr);
            } catch (Exception ignored) {
            }
        }
        String label = getIntent().getStringExtra(EXTRA_LABEL);
        int votes = getIntent().getIntExtra(EXTRA_VOTES, 1);
        int[] proposers = getIntent().getIntArrayExtra(EXTRA_PROPOSERS);
        int drawLevel = getIntent().getIntExtra(EXTRA_DRAW_LEVEL, 0);
        int stickNo = getIntent().getIntExtra(EXTRA_STICK_NO, 1);

        stick = findViewById(R.id.group_winner_image_stick);
        if (stick != null) {
            stick.setColorFilter(new ColorMatrixColorFilter(new float[]{
                    1.07f, 0f, 0f, 0f, 8f,
                    0f, 1.07f, 0f, 0f, 8f,
                    0f, 0f, 1.07f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
            }));
            stick.setRotation(STICK_REST_ROTATION);
        }
        stickLanded = savedInstanceState != null && savedInstanceState.getBoolean(STATE_STICK_LANDED);

        String displayName = label;
        if (method == ProposalMethod.FOOD_TYPE && label != null) {
            String translated = fortuneProvider.getFoodTypeName(label);
            if (translated != null && !translated.isEmpty()) {
                displayName = translated;
            }
        }

        int stars = GameConfig.LEVEL_COUNT - drawLevel;
        String starSymbols = repeatSymbol(R.string.result_label_star, stars);

        setText(R.id.group_winner_label_stick, getString(R.string.result_label_stick, stickNo));
        setText(R.id.group_winner_label_name, displayName);

        if (proposers != null && proposers.length > 0) {
            StringBuilder propsBuilder = new StringBuilder();
            for (int i = 0; i < proposers.length; i++) {
                if (i > 0) {
                    propsBuilder.append(", ");
                }
                propsBuilder.append(getString(R.string.group_tally_person, proposers[i]));
            }
            setText(R.id.group_winner_label_proposers, getString(R.string.group_tally_proposers, propsBuilder.toString()));
        } else {
            setText(R.id.group_winner_label_proposers, "");
        }

        setText(R.id.group_winner_label_votes, getString(R.string.group_winner_votes, votes));
        setText(R.id.group_winner_label_stars, getString(R.string.result_label_stars, starSymbols));

        int fortuneIndex = Math.floorMod(stickNo - 1, 3);
        setText(R.id.group_winner_label_fortune, fortuneProvider.getFortune(stars, fortuneIndex, displayName));

        View btnNewRound = findViewById(R.id.group_winner_btn_new_round);
        if (btnNewRound != null) {
            btnNewRound.setOnClickListener(v -> finish());
        }

        findViewById(R.id.group_winner_btn_home).setOnClickListener(v -> BackNavigation.goHome(this));
        BackNavigation.register(this, () -> BackNavigation.goHome(this));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_STICK_LANDED, stickLanded);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View root = findViewById(R.id.group_winner_root);
        if (root != null) {
            FontSizeHelper.apply(this, root);
        }

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
            if (stick != null) {
                stick.setTranslationY(0f);
                stick.setRotation(STICK_REST_ROTATION);
            }
        }
        super.onPause();
    }

    private String repeatSymbol(int symbol, int count) {
        return String.join("", Collections.nCopies(count, getString(symbol)));
    }

    private void setText(int id, String value) {
        TextView label = findViewById(id);
        if (label != null) {
            label.setText(value);
        }
    }
}
