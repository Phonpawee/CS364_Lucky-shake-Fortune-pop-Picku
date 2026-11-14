package com.cs364.luckyshake;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
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
        findViewById(R.id.home_btn_draw).setOnClickListener(view -> draw());
        View simulate = findViewById(R.id.home_btn_simulate);
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            simulate.setVisibility(View.VISIBLE);
            simulate.setOnClickListener(view -> shakeDetector.simulateShake());
        }
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
        super.onPause();
    }

    private void draw() {
        if (openingResult) {
            return;
        }

        List<Integer> menus = setPicker.getSelectedItemPosition() == 0
                ? RICE_MENUS : NOODLE_MENUS;
        int menu = drawEngine.weightedPick(menus, Arrays.asList(1.0, 1.0));
        openingResult = true;
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_MENU_NAME, menu);
        startActivity(intent);
    }
}
