package com.cs364.luckyshake;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.appcompat.widget.SwitchCompat;

import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuFilter;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.logic.DrawStatus;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.home.DrawState;
import com.cs364.luckyshake.ui.home.HomeSession;
import com.cs364.luckyshake.ui.home.HomeViewModel;
import com.cs364.luckyshake.ui.menu.MenuLabels;
import com.cs364.luckyshake.sensor.ShakeDetector;
import com.cs364.luckyshake.ui.result.ResultActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cs364.luckyshake.ui.nav.BottomNav;
import com.cs364.luckyshake.ui.nav.BackNavigation;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;

public class MainActivity extends AppCompatActivity {
    private static final List<String> MENU_TYPES = Arrays.asList(
            null, Menu.TYPE_RICE, Menu.TYPE_NOODLE, Menu.TYPE_DESSERT,
            Menu.TYPE_DRINK, Menu.TYPE_OTHER);

    private List<MenuSet> menuSets;
    private SharedPreferences filterPreferences;
    private Spinner typePicker;
    private SwitchCompat noFried;
    private SwitchCompat noSpicy;
    private View clearFilters;
    private TextView filterCount;

    private HomeViewModel viewModel;
    private MenuRepository menuRepository;
    private List<MenuSet> sets = new ArrayList<>();
    private ShakeDetector shakeDetector;
    private Spinner setPicker;
    private View drawButton;
    private View tube;
    private static final String STATE_PENDING_DRAW = "pending_draw";
    private View stick;
    private long pendingMenuId;
    private AnimatorSet drawAnimation;
    private AlertDialog exitDialog;
    private boolean openingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        View landscapeScroll = findViewById(R.id.home_landscape_scroll);
        if (landscapeScroll == null) {
            BottomNav.attach(this, R.id.nav_home);
        } else {
            BottomNav.attach(this, R.id.nav_home, landscapeScroll);
        }

        viewModel = HomeViewModel.get(this);
        menuRepository = new MenuRepository(this);
        setPicker = findViewById(R.id.home_spinner_set);
        bindFilters();
        setPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < sets.size()) {
                    viewModel.selectSet(sets.get(position).id);
                    refreshFilterCount();
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
        if (savedInstanceState != null) {
            pendingMenuId = savedInstanceState.getLong(STATE_PENDING_DRAW);
        }
        drawButton = findViewById(R.id.home_btn_draw);
        drawButton.setOnClickListener(view -> draw());
        tube.setOnClickListener(view -> draw());
        tube.setFocusable(true);

        BackNavigation.register(this, this::requestExit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontSizeHelper.apply(this, findViewById(R.id.main));
        reloadSets();   // sets may have been added/renamed/deleted on the Dishes tab
        refreshFilterCount();
        viewModel.setMaxRedraws(getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
                .getInt(PrefKeys.REROLL_QUOTA, GameConfig.MAX_REDRAWS));
        openingResult = false;
        shakeDetector.start();
        resumePendingDraw();
    }

    private void resumePendingDraw() {
        if (exitDialog == null && pendingMenuId != 0L) {
            DrawState pending = viewModel.currentState();
            if (pending.menu != null && pending.menu.id == pendingMenuId) {
                animateDraw();
            } else {
                pendingMenuId = 0L;
            }
        }
    }

    @Override
    protected void onPause() {
        shakeDetector.stop();
        cancelDrawAnimation();
        super.onPause();
    }

    private void cancelDrawAnimation() {
        if (drawAnimation != null) {
            drawAnimation.removeAllListeners();
            drawAnimation.cancel();
            resetDrawAnimation();
        }
    }

    private void requestExit() {
        if (!viewModel.currentState().hasPendingResult()) {
            finish();
            return;
        }
        cancelDrawAnimation();
        exitDialog = BackNavigation.confirmDiscard(this, R.string.back_draw_title,
                R.string.back_draw_message, () -> {
                    pendingMenuId = 0;
                    viewModel.discardPendingResult();
                    finish();
                });
        exitDialog.setOnDismissListener(dialog -> {
            exitDialog = null;
            if (!isFinishing() && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                resumePendingDraw();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong(STATE_PENDING_DRAW, pendingMenuId);
    }

    private void draw() {
        if (exitDialog != null || openingResult || pendingMenuId != 0L) {
            return;
        }

        viewModel.setFilter(currentFilter());
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
        pendingMenuId = result.menu.id;
        animateDraw();
    }

    private void animateDraw() {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishDraw();
            return;
        }
        drawButton.setEnabled(false);
        tube.setEnabled(false);
        setPicker.setEnabled(false);
        setFilterControlsEnabled(false);

        float density = getResources().getDisplayMetrics().density;
        tube.setPivotX(tube.getWidth() / 2f);
        tube.setPivotY(tube.getHeight() * 0.72f);
        AnimatorSet shake = new AnimatorSet();
        shake.playTogether(
                ObjectAnimator.ofFloat(tube, View.ROTATION,
                        0f, -18f, 18f, -16f, 16f, -10f, 10f, 0f),
                ObjectAnimator.ofFloat(tube, View.TRANSLATION_X,
                        0f, -14f * density, 14f * density, -12f * density,
                        12f * density, -6f * density, 6f * density, 0f));
        shake.setDuration(1000);
        shake.setInterpolator(new LinearInterpolator());

        AnimatorSet launchStick = new AnimatorSet();
        launchStick.playTogether(
                ObjectAnimator.ofFloat(stick, View.TRANSLATION_Y, 0f, -68f * density),
                ObjectAnimator.ofFloat(stick, View.TRANSLATION_X, 0f, 39f * density),
                ObjectAnimator.ofFloat(stick, View.ROTATION, 0f, 32f),
                ObjectAnimator.ofFloat(stick, View.SCALE_Y, 0.12f, 1f));
        launchStick.setDuration(320);
        launchStick.setInterpolator(new DecelerateInterpolator());
        launchStick.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                stick.setPivotY(0f);
                stick.setScaleY(0.12f);
                stick.setVisibility(View.VISIBLE);
            }
        });

        drawAnimation = new AnimatorSet();
        drawAnimation.playSequentially(shake, launchStick);
        drawAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishDraw();
            }
        });
        drawAnimation.start();
    }

    private void finishDraw() {
        long menuId = pendingMenuId;
        pendingMenuId = 0L;
        resetDrawAnimation();
        showResult(menuId);
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
        stick.setScaleY(1f);
        drawButton.setEnabled(true);
        setPicker.setEnabled(true);
        setFilterControlsEnabled(true);
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
        setPicker.setAdapter(FontSizeHelper.spinnerAdapter(setPicker.getContext(), names));
        if (sets.isEmpty()) {
            viewModel.selectSet(HomeSession.NO_SET);
        } else {
            setPicker.setSelection(selected);
            viewModel.selectSet(sets.get(selected).id);
        }
    }

    private void bindFilters() {
        filterPreferences = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
        typePicker = findViewById(R.id.home_spinner_type);
        noFried = findViewById(R.id.home_switch_no_fried);
        noSpicy = findViewById(R.id.home_switch_no_spicy);
        clearFilters = findViewById(R.id.home_btn_clear_filters);
        filterCount = findViewById(R.id.home_label_filter_count);

        List<String> typeNames = new ArrayList<>();
        typeNames.add(getString(R.string.home_label_all_types));
        for (String type : MENU_TYPES.subList(1, MENU_TYPES.size())) {
            typeNames.add(MenuLabels.type(this, type));
        }
        typePicker.setAdapter(FontSizeHelper.spinnerAdapter(typePicker.getContext(), typeNames));
        typePicker.setSelection(Math.max(0,
                MENU_TYPES.indexOf(filterPreferences.getString(PrefKeys.FILTER_TYPE, null))));
        noFried.setChecked(filterPreferences.getBoolean(PrefKeys.FILTER_NO_FRIED, false));
        noSpicy.setChecked(filterPreferences.getBoolean(PrefKeys.FILTER_NO_SPICY, false));

        viewModel.setFilter(currentFilter());
        typePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        noFried.setOnCheckedChangeListener((button, checked) -> applyFilters());
        noSpicy.setOnCheckedChangeListener((button, checked) -> applyFilters());
        clearFilters.setOnClickListener(view -> {
            typePicker.setSelection(0);
            noFried.setChecked(false);
            noSpicy.setChecked(false);
            applyFilters();
        });
    }

    private MenuFilter currentFilter() {
        int position = Math.max(0, typePicker.getSelectedItemPosition());
        return new MenuFilter(MENU_TYPES.get(position), noFried.isChecked(), noSpicy.isChecked());
    }

    private void applyFilters() {
        MenuFilter filter = currentFilter();
        viewModel.setFilter(filter);
        filterPreferences.edit()
                .putString(PrefKeys.FILTER_TYPE, filter.type)
                .putBoolean(PrefKeys.FILTER_NO_FRIED, filter.noFried)
                .putBoolean(PrefKeys.FILTER_NO_SPICY, filter.noSpicy)
                .apply();
        refreshFilterCount();
    }

    private void refreshFilterCount() {
        long setId = viewModel.getSelectedSetId();
        int total = menuRepository.countMenus(setId, MenuFilter.none());
        int remaining = menuRepository.countMenus(setId, currentFilter());
        filterCount.setText(getString(R.string.home_label_filter_count, remaining, total));
    }

    private void setFilterControlsEnabled(boolean enabled) {
        typePicker.setEnabled(enabled);
        noFried.setEnabled(enabled);
        noSpicy.setEnabled(enabled);
        clearFilters.setEnabled(enabled);
    }

}
