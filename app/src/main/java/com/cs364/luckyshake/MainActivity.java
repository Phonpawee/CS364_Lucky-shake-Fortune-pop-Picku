package com.cs364.luckyshake;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.logic.DrawStatus;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.home.DrawState;
import com.cs364.luckyshake.ui.home.HomeSession;
import com.cs364.luckyshake.ui.home.HomeViewModel;
import com.cs364.luckyshake.sensor.ShakeDetector;
import com.cs364.luckyshake.ui.result.ResultActivity;

import java.util.ArrayList;
import java.util.List;

import com.cs364.luckyshake.ui.nav.BottomNav;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private HomeViewModel viewModel;
    private MenuRepository menuRepository;
    private List<MenuSet> sets = new ArrayList<>();
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

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(
                                this,
                                R.string.reminder_msg_permission_denied,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BottomNav.attach(this, R.id.nav_home);

        viewModel = HomeViewModel.get(this);
        menuRepository = new MenuRepository(this);
        setPicker = findViewById(R.id.home_spinner_set);
        setPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < sets.size()) {
                    viewModel.selectSet(sets.get(position).id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        shakeDetector = ShakeDetector.create(this);
        shakeDetector.setOnShakeListener(this::draw);
        tube = findViewById(R.id.home_image_tube);
        stick = findViewById(R.id.home_image_stick);
        drawButton = findViewById(R.id.home_btn_draw);
        drawButton.setOnClickListener(view -> draw());
        tube.setOnClickListener(view -> draw());
        tube.setFocusable(true);

        Button permissionButton = findViewById(R.id.reminder_permission_button);
        permissionButton.setOnClickListener(v -> requestNotificationPermission());
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontSizeHelper.apply(this, findViewById(R.id.main));
        reloadSets();   // sets may have been added/renamed/deleted on the Dishes tab
        viewModel.setMaxRedraws(getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                .getInt(PrefKeys.REROLL_QUOTA, GameConfig.MAX_REDRAWS));
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

        DrawState result = viewModel.draw();
        if (!result.hasResult()) {
            // Empty set / nothing passes the filter: no animation, tell the user why.
            Toast.makeText(this, result.status == DrawStatus.NO_MATCH
                    ? R.string.home_msg_no_match : R.string.home_msg_set_empty,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.status == DrawStatus.FEW_ITEMS) {
            Toast.makeText(this, R.string.home_msg_few_items, Toast.LENGTH_SHORT).show();
        }
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
                showResult(result.menu.id);
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

    private void showResult(long menuId) {
        openingResult = true;
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_MENU_ID, menuId);
        startActivity(intent);
    }

    /** Fills the set picker from the database and keeps the chosen set when it still exists. */
    private void reloadSets() {
        sets = menuRepository.getSets();
        boolean thai = "th".equals(
                getResources().getConfiguration().getLocales().get(0).getLanguage());
        List<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < sets.size(); i++) {
            names.add(sets.get(i).getName(thai));
            if (sets.get(i).id == viewModel.getSelectedSetId()) {
                selected = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setPicker.setAdapter(adapter);
        if (sets.isEmpty()) {
            viewModel.selectSet(HomeSession.NO_SET);
        } else {
            setPicker.setSelection(selected);
            viewModel.selectSet(sets.get(selected).id);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
