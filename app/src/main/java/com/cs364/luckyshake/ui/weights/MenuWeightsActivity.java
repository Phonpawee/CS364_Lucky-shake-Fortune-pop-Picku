package com.cs364.luckyshake.ui.weights;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.cs364.luckyshake.data.MenuFilter;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.logic.Candidate;
import com.cs364.luckyshake.logic.DrawEngine;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.logic.WeightPolicy;
import com.cs364.luckyshake.ui.home.HomeViewModel;
import com.cs364.luckyshake.ui.nav.BackNavigation;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Screen displaying the adaptive weight breakdown of all dishes in a menu set (#42).
 * Shows rules, set switcher spinner, dish progress bars, categories, reset weights, and 1,000 draws simulation card.
 */
public class MenuWeightsActivity extends AppCompatActivity {
    public static final String EXTRA_SET_ID = "extra_set_id";
    private static final String STATE_SET_ID = "state_set_id";
    private static final String STATE_SIMULATION_SHOWING = "state_simulation_showing";
    private static final String STATE_SIM_NAMES = "state_sim_names";
    private static final String STATE_SIM_EXPECTED = "state_sim_expected";
    private static final String STATE_SIM_SIMULATED = "state_sim_simulated";

    private MenuRepository menuRepository;
    private MenuWeightsAdapter adapter;
    private View root;
    private Spinner setSpinner;
    private MaterialButton btnReset;
    private MaterialButton btnSimulate;
    private RecyclerView recycler;
    private View emptyGroup;

    // Simulation Card Views & Snapshot
    private View simulationCard;
    private LinearLayout simulationRowsContainer;
    private boolean isSimulationShowing = false;
    private String[] simNames;
    private float[] simExpected;
    private float[] simSimulated;

    private List<MenuSet> sets = new ArrayList<>();
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

        if (savedInstanceState != null) {
            setId = savedInstanceState.getLong(STATE_SET_ID, -1L);
            isSimulationShowing = savedInstanceState.getBoolean(STATE_SIMULATION_SHOWING, false);
            simNames = savedInstanceState.getStringArray(STATE_SIM_NAMES);
            simExpected = savedInstanceState.getFloatArray(STATE_SIM_EXPECTED);
            simSimulated = savedInstanceState.getFloatArray(STATE_SIM_SIMULATED);
        } else {
            setId = getIntent().getLongExtra(EXTRA_SET_ID, -1L);
        }

        if (setId < 0 || menuRepository.getSet(setId) == null) {
            List<MenuSet> available = menuRepository.getSets();
            if (available.isEmpty()) {
                finish();
                return;
            }
            setId = available.get(0).id;
            hideSimulation();
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

        setSpinner = findViewById(R.id.weights_set_spinner);
        TextView ruleAcceptText = findViewById(R.id.weights_text_rule_accept);
        TextView ruleRedrawText = findViewById(R.id.weights_text_rule_redraw);
        TextView ruleDecayText = findViewById(R.id.weights_text_rule_decay);
        btnReset = findViewById(R.id.weights_btn_reset);
        btnSimulate = findViewById(R.id.weights_btn_simulate);
        recycler = findViewById(R.id.weights_recycler);
        emptyGroup = findViewById(R.id.weights_empty_group);

        simulationCard = findViewById(R.id.weights_simulation_card);
        simulationRowsContainer = findViewById(R.id.weights_simulation_rows_container);

        ruleAcceptText.setText(getString(R.string.weights_rule_accept, GameConfig.WEIGHT_ACCEPT_DELTA));
        ruleRedrawText.setText(getString(R.string.weights_rule_redraw, GameConfig.WEIGHT_REDRAW_DELTA));
        ruleDecayText.setText(getString(R.string.weights_rule_decay, GameConfig.WEIGHT_DECAY_STEP, GameConfig.DEFAULT_WEIGHT));

        adapter = new MenuWeightsAdapter(thai);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_menu_item));
        recycler.addItemDecoration(divider);

        if (setSpinner != null) {
            setSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position < 0 || position >= sets.size()) {
                        return;
                    }
                    long newId = sets.get(position).id;
                    if (newId != setId) {
                        setId = newId;
                        hideSimulation();
                        refreshList();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        btnReset.setOnClickListener(v -> {
            hideSimulation();
            confirmResetWeights();
        });
        if (btnSimulate != null) {
            btnSimulate.setOnClickListener(v -> simulateDraws());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SET_ID, setId);
        outState.putBoolean(STATE_SIMULATION_SHOWING, isSimulationShowing);
        if (isSimulationShowing && simNames != null && simExpected != null && simSimulated != null) {
            outState.putStringArray(STATE_SIM_NAMES, simNames);
            outState.putFloatArray(STATE_SIM_EXPECTED, simExpected);
            outState.putFloatArray(STATE_SIM_SIMULATED, simSimulated);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) {
            FontSizeHelper.apply(this, root);
        }
        reloadSets();
        refreshList();
        if (isSimulationShowing) {
            if (renderSimulationRows() && simulationCard != null) {
                simulationCard.setVisibility(View.VISIBLE);
            }
        } else {
            hideSimulation();
        }
    }

    private void reloadSets() {
        sets = menuRepository.getSets();
        if (sets.isEmpty()) {
            finish();
            return;
        }

        boolean thai = isThai();
        List<String> names = new ArrayList<>();
        int selected = 0;
        boolean found = false;

        for (int i = 0; i < sets.size(); i++) {
            MenuSet s = sets.get(i);
            names.add(s.getName(thai));
            if (s.id == setId) {
                selected = i;
                found = true;
            }
        }

        if (setSpinner != null) {
            setSpinner.setAdapter(FontSizeHelper.spinnerAdapter(setSpinner.getContext(), names));
            if (!found) {
                selected = 0;
                setId = sets.get(0).id;
                hideSimulation();
            }
            setSpinner.setSelection(selected);
        }
    }

    private void refreshList() {
        MenuSet set = menuRepository.getSet(setId);
        if (set == null) {
            finish();
            return;
        }

        boolean thai = isThai();
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
        if (btnSimulate != null) {
            btnSimulate.setEnabled(!menusList.isEmpty());
        }

        if (!isSimulationShowing) {
            hideSimulation();
        }

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
                    hideSimulation();
                    refreshList();
                    Snackbar.make(root, R.string.weights_msg_reset_done, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    private void hideSimulation() {
        isSimulationShowing = false;
        simNames = null;
        simExpected = null;
        simSimulated = null;
        if (simulationCard != null) {
            simulationCard.setVisibility(View.GONE);
        }
        if (simulationRowsContainer != null) {
            simulationRowsContainer.removeAllViews();
        }
    }

    private void simulateDraws() {
        simNames = null;
        simExpected = null;
        simSimulated = null;
        if (!renderSimulationRows()) {
            return;
        }

        isSimulationShowing = true;
        if (simulationCard != null) {
            simulationCard.setVisibility(View.VISIBLE);
            simulationCard.post(() -> {
                View scrollView = findViewById(R.id.weights_root);
                if (scrollView != null) {
                    scrollView.scrollTo(0, simulationCard.getBottom());
                }
            });
        }
    }

    private boolean renderSimulationRows() {
        if (simNames == null || simExpected == null || simSimulated == null) {
            MenuFilter homeFilter = HomeViewModel.get(this).getFilter();
            List<Menu> filteredMenus = menuRepository.getMenus(setId, homeFilter, null);
            if (filteredMenus.isEmpty()) {
                hideSimulation();
                return false;
            }

            long now = System.currentTimeMillis();
            int size = filteredMenus.size();
            int[] decayedWeights = new int[size];
            double totalWeight = 0;

            for (int i = 0; i < size; i++) {
                Menu m = filteredMenus.get(i);
                int w = WeightPolicy.decay(m.weight, m.lastTouched, now);
                decayedWeights[i] = w;
                totalWeight += w;
            }

            List<Candidate> candidates = new ArrayList<>(size);
            List<Double> weights = new ArrayList<>(size);
            Map<Long, Integer> counts = new HashMap<>();

            for (int i = 0; i < size; i++) {
                Menu m = filteredMenus.get(i);
                candidates.add(new Candidate(m.id, decayedWeights[i]));
                weights.add((double) decayedWeights[i]);
                counts.put(m.id, 0);
            }

            DrawEngine engine = DrawEngine.create();
            int totalRuns = 1000;

            for (int i = 0; i < totalRuns; i++) {
                Candidate picked = engine.weightedPick(candidates, weights);
                if (picked != null) {
                    Integer current = counts.get(picked.id);
                    counts.put(picked.id, (current != null ? current : 0) + 1);
                }
            }

            boolean thai = isThai();
            simNames = new String[size];
            simExpected = new float[size];
            simSimulated = new float[size];

            for (int i = 0; i < size; i++) {
                Menu m = filteredMenus.get(i);
                simNames[i] = m.getName(thai);
                simExpected[i] = (float) (totalWeight > 0 ? (decayedWeights[i] * 100.0) / totalWeight : 0.0);
                Integer cnt = counts.get(m.id);
                int count = cnt != null ? cnt : 0;
                simSimulated[i] = (float) ((count * 100.0) / totalRuns);
            }
        }

        if (simulationRowsContainer != null) {
            simulationRowsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < simNames.length; i++) {
                View row = inflater.inflate(R.layout.item_menu_simulation_row, simulationRowsContainer, false);
                TextView textName = row.findViewById(R.id.text_sim_name);
                TextView textExpected = row.findViewById(R.id.text_sim_expected);
                TextView textSimulated = row.findViewById(R.id.text_sim_simulated);

                textName.setText(simNames[i]);
                textExpected.setText(String.format(Locale.US, "%.1f%%", simExpected[i]));
                textSimulated.setText(String.format(Locale.US, "%.1f%%", simSimulated[i]));

                FontSizeHelper.apply(this, row);
                simulationRowsContainer.addView(row);
            }
        }
        return true;
    }

    private boolean isThai() {
        return "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
