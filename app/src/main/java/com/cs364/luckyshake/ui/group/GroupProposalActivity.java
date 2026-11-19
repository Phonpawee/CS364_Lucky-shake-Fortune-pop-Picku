package com.cs364.luckyshake.ui.group;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.fortune.FortuneProvider;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.sensor.ShakeDetector;
import com.cs364.luckyshake.ui.nav.BackNavigation;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;

/**
 * Screen for collecting restaurant or food group proposals per participant (#36, #35),
 * rendering vote summary tally and drawing weighted winner (#37), and managing Back confirmation (#38).
 */
public class GroupProposalActivity extends AppCompatActivity {
    public static final String EXTRA_PARTICIPANT_COUNT = "extra_participant_count";
    public static final String EXTRA_PROPOSAL_METHOD = "extra_proposal_method";

    private GroupProposalViewModel viewModel;
    private View root;
    private TextView titleTurn;
    private TextView instructions;
    private View inputCard;
    private View buttonPair;
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

    // Tally & Draw Views (#37)
    private View tallySection;
    private RecyclerView tallyList;
    private MaterialButton tallyBtnDraw;
    private GroupTallyAdapter tallyAdapter;
    private ShakeDetector shakeDetector;

    private ProposalMethod proposalMethod = ProposalMethod.FOOD_TYPE;
    private AlertDialog cancelDialog;

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
        viewModel.setHistoryRecorder(entry -> new com.cs364.luckyshake.data.HistoryRepository(this).add(entry) > 0);
        viewModel.setFortuneProvider(new FortuneProvider(this));

        // #14: Back navigation (Mind) can call requestCancel() / hasUnsavedProgress()
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestCancel();
            }
        });

        getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_RESUME) {
                maybeStartShakeDetection();
            }
        });

        titleTurn = findViewById(R.id.group_proposal_title_turn);
        instructions = findViewById(R.id.group_proposal_instructions);
        inputCard = findViewById(R.id.group_proposal_input_card);
        buttonPair = findViewById(R.id.group_proposal_button_pair);
        restaurantGroup = findViewById(R.id.group_proposal_restaurant_group);
        foodGrid = findViewById(R.id.group_proposal_food_grid);
        foodError = findViewById(R.id.group_proposal_food_error);

        layoutRestaurant1 = findViewById(R.id.group_proposal_layout_restaurant_1);
        editRestaurant1 = findViewById(R.id.group_proposal_edit_restaurant_1);
        editRestaurant2 = findViewById(R.id.group_proposal_edit_restaurant_2);
        editRestaurant3 = findViewById(R.id.group_proposal_edit_restaurant_3);
        btnSkip = findViewById(R.id.group_proposal_btn_skip);
        btnConfirm = findViewById(R.id.group_proposal_btn_confirm);

        // Tally & Winner Views (#37)
        tallySection = findViewById(R.id.group_tally_section);
        tallyList = findViewById(R.id.group_tally_list);
        tallyBtnDraw = findViewById(R.id.group_tally_btn_draw);

        shakeDetector = ShakeDetector.create(this);
        shakeDetector.setOnShakeListener(() -> {
            if ((cancelDialog == null || !cancelDialog.isShowing()) && !isFinishing() && !viewModel.isExitConfirmShowing()) {
                viewModel.drawWinner();
            }
        });

        if (tallyBtnDraw != null) {
            tallyBtnDraw.setOnClickListener(v -> viewModel.drawWinner());
        }

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

        if (viewModel.isExitConfirmShowing()) {
            requestCancel();
        }
    }

    /**
     * Handles Back button / cancellation confirmation dialog (#38).
     */
    public void requestCancel() {
        if (!viewModel.hasUnsavedProgress()) {
            if (viewModel.getPhase().getValue() == GroupPhase.DONE) {
                BackNavigation.goHome(this);
            } else {
                finish();
            }
            return;
        }

        if (cancelDialog != null && cancelDialog.isShowing()) {
            return;
        }

        if (shakeDetector != null) {
            shakeDetector.stop();
        }

        viewModel.setExitConfirmShowing(true);
        AlertDialog dialogToBuild = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.group_cancel_title)
                .setMessage(R.string.group_cancel_msg)
                .setPositiveButton(R.string.group_cancel_btn_discard, (dialog, which) -> {
                    if (cancelDialog == dialog) {
                        viewModel.setExitConfirmShowing(false);
                        viewModel.cancelRound();
                        finish();
                    }
                })
                .setNegativeButton(R.string.group_cancel_btn_stay, (dialog, which) -> {
                    if (cancelDialog == dialog) {
                        viewModel.setExitConfirmShowing(false);
                        maybeStartShakeDetection();
                    }
                })
                .setOnCancelListener(dialog -> {
                    if (cancelDialog == dialog) {
                        viewModel.setExitConfirmShowing(false);
                        maybeStartShakeDetection();
                    }
                })
                .show();

        cancelDialog = dialogToBuild;
    }

    private void maybeStartShakeDetection() {
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            return;
        }
        if (viewModel.isExitConfirmShowing()) {
            return;
        }
        if (viewModel.getWinner().getValue() != null) {
            return;
        }
        GroupPhase currentPhase = viewModel.getPhase().getValue();
        if ((currentPhase == GroupPhase.FINAL || currentPhase == GroupPhase.DONE)
                && shakeDetector != null) {
            shakeDetector.start();
        }
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

        viewModel.getTally().observe(this, tallyOptions -> {
            if (tallyAdapter != null && tallyOptions != null) {
                tallyAdapter.submitList(tallyOptions);
            }
        });

        viewModel.getPhase().observe(this, phase -> {
            updateTitleHeader();
            if (phase == GroupPhase.FINAL || phase == GroupPhase.DONE) {
                if (viewModel.isTallyEmpty()) {
                    Toast.makeText(this, R.string.group_tally_msg_all_skipped, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                if (inputCard != null) inputCard.setVisibility(View.GONE);
                if (buttonPair != null) buttonPair.setVisibility(View.GONE);
                if (tallySection != null) tallySection.setVisibility(View.VISIBLE);

                if (tallyAdapter == null && tallyList != null) {
                    tallyAdapter = new GroupTallyAdapter(this, proposalMethod);
                    tallyList.setLayoutManager(new LinearLayoutManager(this));
                    tallyList.setAdapter(tallyAdapter);
                    if (viewModel.getTally().getValue() != null) {
                        tallyAdapter.submitList(viewModel.getTally().getValue());
                    }
                }

                maybeStartShakeDetection();
            }
        });

        viewModel.getWinner().observe(this, winner -> {
            if (winner != null && !isFinishing()) {
                shakeDetector.stop();
                int[] proposers = new int[winner.proposers.size()];
                for (int i = 0; i < proposers.length; i++) {
                    proposers[i] = winner.proposers.get(i);
                }
                startActivity(GroupWinnerActivity.newIntent(this, proposalMethod, winner.label,
                        winner.votes, proposers, viewModel.getDrawLevel(), viewModel.getStickNo()));
                finish();
            }
        });
    }

    private void updateTitleHeader() {
        GroupPhase phase = viewModel.getPhase().getValue();
        if (phase == GroupPhase.FINAL || phase == GroupPhase.DONE) {
            titleTurn.setText(R.string.group_tally_title);
            return;
        }

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

    @Override
    protected void onPause() {
        super.onPause();
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
        if (cancelDialog != null && cancelDialog.isShowing()) {
            cancelDialog.dismiss();
            cancelDialog = null;
        }
    }
}
