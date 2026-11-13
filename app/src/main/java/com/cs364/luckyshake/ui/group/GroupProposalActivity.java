package com.cs364.luckyshake.ui.group;

import android.content.Context;
import android.content.Intent;
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

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.logic.GameConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;

/**
 * Screen for collecting restaurant proposals per participant (#36).
 * Shows inputs for up to 3 restaurant names and passes the device to the next person.
 */
public class GroupProposalActivity extends AppCompatActivity {
    public static final String EXTRA_PARTICIPANT_COUNT = "extra_participant_count";
    public static final String EXTRA_PROPOSAL_METHOD = "extra_proposal_method";

    private GroupProposalViewModel viewModel;
    private View root;
    private TextView titleTurn;
    private TextInputLayout layoutRestaurant1;
    private TextInputEditText editRestaurant1;
    private TextInputEditText editRestaurant2;
    private TextInputEditText editRestaurant3;
    private MaterialButton btnSkip;
    private MaterialButton btnConfirm;

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
        ProposalMethod method = ProposalMethod.FOOD_TYPE;
        if (methodStr != null) {
            try {
                method = ProposalMethod.valueOf(methodStr);
            } catch (Exception ignored) {
            }
        }

        viewModel = new ViewModelProvider(this).get(GroupProposalViewModel.class);

        titleTurn = findViewById(R.id.group_proposal_title_turn);
        layoutRestaurant1 = findViewById(R.id.group_proposal_layout_restaurant_1);
        editRestaurant1 = findViewById(R.id.group_proposal_edit_restaurant_1);
        editRestaurant2 = findViewById(R.id.group_proposal_edit_restaurant_2);
        editRestaurant3 = findViewById(R.id.group_proposal_edit_restaurant_3);
        btnSkip = findViewById(R.id.group_proposal_btn_skip);
        btnConfirm = findViewById(R.id.group_proposal_btn_confirm);

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutRestaurant1.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        editRestaurant1.addTextChangedListener(clearErrorWatcher);
        editRestaurant2.addTextChangedListener(clearErrorWatcher);
        editRestaurant3.addTextChangedListener(clearErrorWatcher);

        btnSkip.setOnClickListener(v -> handleSkip());
        btnConfirm.setOnClickListener(v -> handleConfirm());

        observeViewModel();
        viewModel.init(count, method);
    }

    private void observeViewModel() {
        viewModel.getCurrentTurnNumber().observe(this, turn -> updateTitleHeader());
        viewModel.getParticipantCount().observe(this, total -> updateTitleHeader());

        viewModel.getPhase().observe(this, phase -> {
            if (phase == GroupPhase.FINAL) {
                editRestaurant1.setEnabled(false);
                editRestaurant2.setEnabled(false);
                editRestaurant3.setEnabled(false);
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
            layoutRestaurant1.setError(null);
            editRestaurant1.setText("");
            editRestaurant2.setText("");
            editRestaurant3.setText("");

            if (viewModel.getPhase().getValue() != GroupPhase.FINAL) {
                Snackbar.make(root, R.string.group_proposal_msg_pass_phone, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void handleConfirm() {
        String r1 = editRestaurant1.getText() != null ? editRestaurant1.getText().toString() : "";
        String r2 = editRestaurant2.getText() != null ? editRestaurant2.getText().toString() : "";
        String r3 = editRestaurant3.getText() != null ? editRestaurant3.getText().toString() : "";

        boolean ok = viewModel.submitRestaurants(Arrays.asList(r1, r2, r3));
        if (!ok) {
            layoutRestaurant1.setError(getString(R.string.group_proposal_error_restaurant_empty));
        } else {
            layoutRestaurant1.setError(null);
            editRestaurant1.setText("");
            editRestaurant2.setText("");
            editRestaurant3.setText("");

            if (viewModel.getPhase().getValue() != GroupPhase.FINAL) {
                Snackbar.make(root, R.string.group_proposal_msg_pass_phone, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
