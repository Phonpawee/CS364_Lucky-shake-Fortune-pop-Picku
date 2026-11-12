package com.cs364.luckyshake.ui.group;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

/**
 * Setup screen for group decision mode (#34).
 * Allows users to choose the number of participants (2-8) and the proposal method (Food Category or Restaurant Name).
 */
public class GroupSetupActivity extends AppCompatActivity {

    private GroupViewModel viewModel;
    private View root;
    private TextView rangeLabel;
    private TextView countText;
    private MaterialButton btnDecrement;
    private MaterialButton btnIncrement;
    private MaterialButtonToggleGroup toggleGroupMethod;
    private TextView textMethodHint;
    private MaterialButton btnStart;

    public static Intent newIntent(Context context) {
        return new Intent(context, GroupSetupActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_setup);

        root = findViewById(R.id.group_setup_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        rangeLabel = findViewById(R.id.group_setup_label_range);
        countText = findViewById(R.id.group_setup_text_count);
        btnDecrement = findViewById(R.id.group_setup_btn_decrement);
        btnIncrement = findViewById(R.id.group_setup_btn_increment);
        toggleGroupMethod = findViewById(R.id.group_setup_toggle_group_method);
        textMethodHint = findViewById(R.id.text_method_hint);
        btnStart = findViewById(R.id.group_setup_btn_start);

        rangeLabel.setText(getString(R.string.group_setup_label_range,
                GameConfig.MIN_GROUP_PLAYERS, GameConfig.MAX_GROUP_PLAYERS));

        btnDecrement.setOnClickListener(v -> viewModel.decrementParticipantCount());
        btnIncrement.setOnClickListener(v -> viewModel.incrementParticipantCount());

        toggleGroupMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.group_setup_btn_method_food_type) {
                    viewModel.setProposalMethod(ProposalMethod.FOOD_TYPE);
                } else if (checkedId == R.id.group_setup_btn_method_restaurant_name) {
                    viewModel.setProposalMethod(ProposalMethod.RESTAURANT_NAME);
                }
            }
        });

        btnStart.setOnClickListener(v -> handleStartRound());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getParticipantCount().observe(this, count -> {
            if (count != null) {
                countText.setText(getString(R.string.group_setup_label_count, count));
            }
        });

        viewModel.getCanDecrement().observe(this, canDec -> {
            if (canDec != null) {
                btnDecrement.setEnabled(canDec);
            }
        });

        viewModel.getCanIncrement().observe(this, canInc -> {
            if (canInc != null) {
                btnIncrement.setEnabled(canInc);
            }
        });

        viewModel.getProposalMethod().observe(this, method -> {
            if (method == ProposalMethod.FOOD_TYPE) {
                toggleGroupMethod.check(R.id.group_setup_btn_method_food_type);
                textMethodHint.setText(R.string.group_setup_msg_method_food_type);
            } else if (method == ProposalMethod.RESTAURANT_NAME) {
                toggleGroupMethod.check(R.id.group_setup_btn_method_restaurant_name);
                textMethodHint.setText(R.string.group_setup_msg_method_restaurant);
            }
        });
    }

    private void handleStartRound() {
        if (viewModel.startGroupSession()) {
            Snackbar.make(root, R.string.group_setup_msg_started, Snackbar.LENGTH_SHORT).show();
            // TODO(#35/#36): Navigate to proposal collection screen once implemented.
            // Reset back to SETUP for now so another test round can be configured.
            viewModel.resetSession();
        }
    }
}
