package com.cs364.luckyshake.ui.weights;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.nav.BackNavigation;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Screen displaying the adaptive weight breakdown of all dishes in a menu set (#42).
 * Shows rules, dish progress bars, categories, and option to reset set weights back to default.
 */
public class MenuWeightsActivity extends AppCompatActivity {
    public static final String EXTRA_SET_ID = "extra_set_id";

    private MenuRepository menuRepository;
    private MenuWeightsAdapter adapter;
    private View root;
    private TextView textSetName;
    private MaterialButton btnReset;
    private RecyclerView recycler;
    private View emptyGroup;

    private long setId = -1;

    public static Intent newIntent(Context context, long setId) {
        Intent intent = new Intent(context, MenuWeightsActivity.class);
        intent.putExtra(EXTRA_SET_ID, setId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        menuRepository = new MenuRepository(this);

        setId = getIntent().getLongExtra(EXTRA_SET_ID, -1L);
        if (setId < 0 || menuRepository.getSet(setId) == null) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_weights);

        root = findViewById(R.id.weights_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BackNavigation.register(this, this::finish);

        boolean thai = isThai();

        textSetName = findViewById(R.id.weights_text_set_name);
        TextView ruleAcceptText = findViewById(R.id.weights_text_rule_accept);
        TextView ruleRedrawText = findViewById(R.id.weights_text_rule_redraw);
        TextView ruleDecayText = findViewById(R.id.weights_text_rule_decay);
        btnReset = findViewById(R.id.weights_btn_reset);
        recycler = findViewById(R.id.weights_recycler);
        emptyGroup = findViewById(R.id.weights_empty_group);

        ruleAcceptText.setText(getString(R.string.weights_rule_accept, GameConfig.WEIGHT_ACCEPT_DELTA));
        ruleRedrawText.setText(getString(R.string.weights_rule_redraw, GameConfig.WEIGHT_REDRAW_DELTA));
        ruleDecayText.setText(getString(R.string.weights_rule_decay, GameConfig.WEIGHT_DECAY_STEP, GameConfig.DEFAULT_WEIGHT));

        adapter = new MenuWeightsAdapter(thai);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_menu_item));
        recycler.addItemDecoration(divider);

        btnReset.setOnClickListener(v -> confirmResetWeights());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) {
            FontSizeHelper.apply(this, root);
        }
        refreshList();
    }

    private void refreshList() {
        MenuSet set = menuRepository.getSet(setId);
        if (set == null) {
            finish();
            return;
        }

        boolean thai = isThai();
        textSetName.setText(getString(
                R.string.menu_weights_label_set_name, set.getName(thai)));

        List<Menu> menusList = menuRepository.getMenus(setId, null, null);
        Collections.sort(menusList, (a, b) -> {
            int weightDiff = Integer.compare(b.weight, a.weight); // DESC
            if (weightDiff != 0) {
                return weightDiff;
            }
            return a.getName(thai).compareToIgnoreCase(b.getName(thai)); // ASC
        });

        List<Integer> weightsList = new ArrayList<>();
        for (Menu m : menusList) {
            weightsList.add(m.weight);
        }

        boolean allDefault = WeightLabels.allDefault(weightsList);
        btnReset.setEnabled(!allDefault && !menusList.isEmpty());

        adapter.submitList(menusList);

        boolean empty = menusList.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void confirmResetWeights() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.weights_reset_title)
                .setMessage(R.string.weights_reset_message)
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.weights_btn_reset, (dialog, which) -> {
                    menuRepository.resetWeights(setId);
                    refreshList();
                    Snackbar.make(root, R.string.weights_msg_reset_done, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    private boolean isThai() {
        return "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
