package com.cs364.luckyshake.ui.group;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;

/**
 * Screen for collecting restaurant or food group proposals per participant (#36, #35).
 * Shows food category grid or restaurant input fields, passing the device to the next person.
 */
public class GroupProposalActivity extends AppCompatActivity {
    public static final String EXTRA_PARTICIPANT_COUNT = "extra_participant_count";
    public static final String EXTRA_PROPOSAL_METHOD = "extra_proposal_method";

    private GroupProposalViewModel viewModel;
    private View root;
    private TextView titleTurn;
    private TextView instructions;
    private View restaurantGroup;
    private RecyclerView foodGrid;
    private TextView foodError;
    private FoodTypeAdapter foodTypeAdapter;

    private TextInputLayout layoutRestaurant1;
    private TextInputEditText editRestaurant1;
    private TextInputEditText editRestaurant2;
    private TextInputEditText editRestaurant3;
    private MaterialButton btnSkip;
    private MaterialButton btnConfirm;

    private ProposalMethod proposalMethod = ProposalMethod.FOOD_TYPE;

    public static Intent newIntent(Context context, int participantCount, ProposalMethod proposalMethod) {
        Intent intent = new Intent(context, GroupProposalActivity.class);
        intent.putExtra(EXTRA_PARTICIPANT_COUNT, participantCount);
        intent.putExtra(EXTRA_PROPOSAL_METHOD, proposalMethod != null ? proposalMethod.name() : ProposalMethod.FOOD_TYPE.name());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_proposal);

        root = findViewById(R.id.group_proposal_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        int count = getIntent().getIntExtra(EXTRA_PARTICIPANT_COUNT, GameConfig.MIN_GROUP_PLAYERS);
        String methodStr = getIntent().getStringExtra(EXTRA_PROPOSAL_METHOD);
        if (methodStr != null) {
            try {
                proposalMethod = ProposalMethod.valueOf(methodStr);
            } catch (Exception ignored) {
            }
        }

        viewModel = new ViewModelProvider(this).get(GroupProposalViewModel.class);

        titleTurn = findViewById(R.id.group_proposal_title_turn);
        instructions = findViewById(R.id.group_proposal_instructions);
        restaurantGroup = findViewById(R.id.group_proposal_restaurant_group);
        foodGrid = findViewById(R.id.group_proposal_food_grid);
        foodError = findViewById(R.id.group_proposal_food_error);

        layoutRestaurant1 = findViewById(R.id.group_proposal_layout_restaurant_1);
        editRestaurant1 = findViewById(R.id.group_proposal_edit_restaurant_1);
        editRestaurant2 = findViewById(R.id.group_proposal_edit_restaurant_2);
        editRestaurant3 = findViewById(R.id.group_proposal_edit_restaurant_3);
        btnSkip = findViewById(R.id.group_proposal_btn_skip);
        btnConfirm = findViewById(R.id.group_proposal_btn_confirm);

        if (proposalMethod == ProposalMethod.FOOD_TYPE) {
            if (restaurantGroup != null) restaurantGroup.setVisibility(View.GONE);
            if (foodGrid != null) {
                foodGrid.setVisibility(View.VISIBLE);
                boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                foodGrid.setLayoutManager(new GridLayoutManager(this, landscape ? 3 : 2));
                foodTypeAdapter = new FoodTypeAdapter(this, key -> viewModel.selectFoodType(key));
                foodGrid.setAdapter(foodTypeAdapter);
            }
            if (instructions != null) {
                instructions.setText(R.string.group_proposal_msg_food_hint);
            }
        } else {
            if (restaurantGroup != null) restaurantGroup.setVisibility(View.VISIBLE);
            if (foodGrid != null) foodGrid.setVisibility(View.GONE);
            if (instructions != null) {
                instructions.setText(R.string.group_proposal_msg_restaurant_hint);
            }
        }

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layoutRestaurant1 != null) {
                    layoutRestaurant1.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        if (editRestaurant1 != null) editRestaurant1.addTextChangedListener(clearErrorWatcher);
        if (editRestaurant2 != null) editRestaurant2.addTextChangedListener(clearErrorWatcher);
        if (editRestaurant3 != null) editRestaurant3.addTextChangedListener(clearErrorWatcher);

        btnSkip.setOnClickListener(v -> handleSkip());
        btnConfirm.setOnClickListener(v -> handleConfirm());

        observeViewModel();
        viewModel.init(count, proposalMethod);
    }

    private void observeViewModel() {
        viewModel.getCurrentTurnNumber().observe(this, turn -> updateTitleHeader());
        viewModel.getParticipantCount().observe(this, total -> updateTitleHeader());

        viewModel.getSelectedFoodType().observe(this, selectedKey -> {
            if (foodTypeAdapter != null) {
                foodTypeAdapter.setSelectedKey(selectedKey);
            }
            if (selectedKey != null && foodError != null) {
                foodError.setVisibility(View.GONE);
            }
        });

        viewModel.getPhase().observe(this, phase -> {
            if (phase == GroupPhase.FINAL) {
                if (editRestaurant1 != null) editRestaurant1.setEnabled(false);
                if (editRestaurant2 != null) editRestaurant2.setEnabled(false);
                if (editRestaurant3 != null) editRestaurant3.setEnabled(false);
                if (foodGrid != null) foodGrid.setEnabled(false);
                btnSkip.setEnabled(false);
                btnConfirm.setEnabled(false);
                Snackbar.make(root, R.string.group_proposal_msg_all_done, Snackbar.LENGTH_LONG).show();
                // TODO(#37): Navigate to vote tally and draw winner screen once implemented.
            }
        });
    }

    private void updateTitleHeader() {
        Integer turn = viewModel.getCurrentTurnNumber().getValue();
        Integer total = viewModel.getParticipantCount().getValue();
        if (turn != null && total != null) {
            titleTurn.setText(getString(R.string.group_proposal_title_turn, turn, total));
        }
    }

    private void handleSkip() {
        boolean ok = viewModel.skipTurn();
        if (ok) {
            if (layoutRestaurant1 != null) layoutRestaurant1.setError(null);
            if (editRestaurant1 != null) editRestaurant1.setText("");
            if (editRestaurant2 != null) editRestaurant2.setText("");
            if (editRestaurant3 != null) editRestaurant3.setText("");
            if (foodError != null) foodError.setVisibility(View.GONE);

            if (viewModel.getPhase().getValue() != GroupPhase.FINAL) {
                Snackbar.make(root, R.string.group_proposal_msg_pass_phone, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void handleConfirm() {
        if (proposalMethod == ProposalMethod.FOOD_TYPE) {
            boolean ok = viewModel.submitFoodType();
            if (!ok) {
                if (foodError != null) {
                    foodError.setVisibility(View.VISIBLE);
                    FontSizeHelper.apply(this, foodError);
                }
            } else {
                if (foodError != null) foodError.setVisibility(View.GONE);
                if (viewModel.getPhase().getValue() != GroupPhase.FINAL) {
                    Snackbar.make(root, R.string.group_proposal_msg_pass_phone, Snackbar.LENGTH_SHORT).show();
                }
            }
        } else {
            String r1 = editRestaurant1 != null && editRestaurant1.getText() != null ? editRestaurant1.getText().toString() : "";
            String r2 = editRestaurant2 != null && editRestaurant2.getText() != null ? editRestaurant2.getText().toString() : "";
            String r3 = editRestaurant3 != null && editRestaurant3.getText() != null ? editRestaurant3.getText().toString() : "";

            boolean ok = viewModel.submitRestaurants(Arrays.asList(r1, r2, r3));
            if (!ok) {
                if (layoutRestaurant1 != null) {
                    layoutRestaurant1.setError(getString(R.string.group_proposal_error_restaurant_empty));
                    FontSizeHelper.apply(this, layoutRestaurant1);
                }
            } else {
                if (layoutRestaurant1 != null) layoutRestaurant1.setError(null);
                if (editRestaurant1 != null) editRestaurant1.setText("");
                if (editRestaurant2 != null) editRestaurant2.setText("");
                if (editRestaurant3 != null) editRestaurant3.setText("");

                if (viewModel.getPhase().getValue() != GroupPhase.FINAL) {
                    Snackbar.make(root, R.string.group_proposal_msg_pass_phone, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontSizeHelper.apply(this, root);
    }
}
