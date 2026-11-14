package com.cs364.luckyshake;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.logic.DrawEngine;
import com.cs364.luckyshake.sensor.ShakeDetector;
import com.cs364.luckyshake.ui.result.ResultActivity;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Temporary menu sets for F-01, until the Home screen is wired to MenuRepository.
    private static final List<Integer> RICE_MENUS =
            Arrays.asList(R.string.home_label_basil, R.string.home_label_omelette);
    private static final List<Integer> NOODLE_MENUS =
            Arrays.asList(R.string.home_label_boat_noodles, R.string.home_label_pad_thai);

    private final DrawEngine drawEngine = DrawEngine.create();
    private ShakeDetector shakeDetector;
    private Spinner setPicker;
    private View drawButton;
    private View tube;
    private View stick;
    private AnimatorSet drawAnimation;
    private boolean openingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setPicker = findViewById(R.id.home_spinner_set);
        ArrayAdapter<String> sets = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(getString(R.string.home_label_rice),
                        getString(R.string.home_label_noodles)));
        sets.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setPicker.setAdapter(sets);

        shakeDetector = ShakeDetector.create(this);
        shakeDetector.setOnShakeListener(this::draw);
        tube = findViewById(R.id.home_image_tube);
        stick = findViewById(R.id.home_image_stick);
        drawButton = findViewById(R.id.home_btn_draw);
        drawButton.setOnClickListener(view -> draw());
        tube.setOnClickListener(view -> draw());
        tube.setFocusable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openingResult = false;
        shakeDetector.start();
    }

    @Override
    protected void onPause() {
        shakeDetector.stop();
        if (drawAnimation != null) {
            drawAnimation.removeAllListeners();
            drawAnimation.cancel();
            resetDrawAnimation();
        }
        super.onPause();
    }

    private void draw() {
        if (openingResult || drawAnimation != null) {
            return;
        }

        List<Integer> menus = setPicker.getSelectedItemPosition() == 0
                ? RICE_MENUS : NOODLE_MENUS;
        int menu = drawEngine.weightedPick(menus, Arrays.asList(1.0, 1.0));
        drawButton.setEnabled(false);
        tube.setEnabled(false);
        setPicker.setEnabled(false);

        float density = getResources().getDisplayMetrics().density;
        AnimatorSet shake = new AnimatorSet();
        shake.playTogether(
                ObjectAnimator.ofFloat(tube, View.ROTATION,
                        0f, -18f, 18f, -16f, 16f, -10f, 10f, 0f),
                ObjectAnimator.ofFloat(tube, View.TRANSLATION_X,
                        0f, -14f * density, 14f * density, -12f * density,
                        12f * density, -6f * density, 6f * density, 0f));
        shake.setDuration(620);
        shake.setInterpolator(new LinearInterpolator());

        AnimatorSet pop = new AnimatorSet();
        pop.playTogether(
                ObjectAnimator.ofFloat(stick, View.TRANSLATION_X, 0f, 30f * density, 77f * density),
                ObjectAnimator.ofFloat(stick, View.TRANSLATION_Y, 0f, -63f * density, 94f * density),
                ObjectAnimator.ofFloat(stick, View.ROTATION, 0f, 30f, 65f));
        pop.setDuration(620);
        pop.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                stick.setVisibility(View.VISIBLE);
            }
        });

        drawAnimation = new AnimatorSet();
        drawAnimation.playSequentially(shake, pop);
        drawAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetDrawAnimation();
                showResult(menu);
            }
        });
        drawAnimation.start();
    }

    private void resetDrawAnimation() {
        drawAnimation = null;
        tube.setRotation(0f);
        tube.setTranslationX(0f);
        tube.setEnabled(true);
        stick.setVisibility(View.INVISIBLE);
        stick.setTranslationX(0f);
        stick.setTranslationY(0f);
        stick.setRotation(0f);
        drawButton.setEnabled(true);
        setPicker.setEnabled(true);
    }

    private void showResult(int menu) {
        openingResult = true;
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_MENU_NAME, menu);
        startActivity(intent);
    }
}
