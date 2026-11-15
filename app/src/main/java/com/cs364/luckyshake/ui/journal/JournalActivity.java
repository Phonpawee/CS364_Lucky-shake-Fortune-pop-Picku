package com.cs364.luckyshake.ui.journal;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.HistoryEntry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

/**
 * Screen displaying the user's fortune draw history (#40).
 * Supports filtering entries, deleting individual items, clearing all entries with a confirmation dialog,
 * and rendering empty state messages.
 */
public class JournalActivity extends AppCompatActivity {

    private JournalViewModel viewModel;
    private View root;
    private ChipGroup chipGroup;
    private MaterialButton btnClearAll;
    private RecyclerView recycler;
    private JournalAdapter adapter;
    private View emptyGroup;
    private TextView emptyText;

    public static Intent newIntent(Context context) {
        return new Intent(context, JournalActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_journal);

        root = findViewById(R.id.journal_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Locale locale = getResources().getConfiguration().getLocales().get(0);
        boolean thai = "th".equals(locale.getLanguage());

        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);

        chipGroup = findViewById(R.id.journal_chip_group);
        btnClearAll = findViewById(R.id.journal_btn_clear_all);
        recycler = findViewById(R.id.journal_recycler);
        emptyGroup = findViewById(R.id.journal_empty_group);
        emptyText = findViewById(R.id.journal_empty_text);

        adapter = new JournalAdapter(this, thai, locale, entry -> {
            viewModel.delete(entry.id);
            Snackbar.make(root, R.string.journal_msg_deleted, Snackbar.LENGTH_SHORT).show();
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        MaterialDividerItemDecoration divider =
                new MaterialDividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        int inset = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        divider.setDividerInsetStart(inset);
        divider.setDividerInsetEnd(inset);
        divider.setDividerColor(ContextCompat.getColor(this, R.color.journal_outline));
        divider.setLastItemDecorated(false);
        recycler.addItemDecoration(divider);

        MaterialButtonToggleGroup toggleGroupTab = findViewById(R.id.journal_toggle_group_tab);
        if (toggleGroupTab != null) {
            toggleGroupTab.check(R.id.journal_btn_tab_entries);
            toggleGroupTab.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked && checkedId == R.id.journal_btn_tab_stats) {
                    Snackbar.make(root, R.string.journal_msg_stats_pending, Snackbar.LENGTH_SHORT).show();
                    toggleGroupTab.check(R.id.journal_btn_tab_entries);
                }
            });
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.journal_chip_all) {
                viewModel.setFilter(JournalViewModel.Filter.ALL);
            } else if (checkedId == R.id.journal_chip_accepted) {
                viewModel.setFilter(JournalViewModel.Filter.ACCEPTED);
            } else if (checkedId == R.id.journal_chip_redrawn) {
                viewModel.setFilter(JournalViewModel.Filter.REDRAWN);
            }
        });

        btnClearAll.setOnClickListener(v -> confirmClearAll());

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void observeViewModel() {
        viewModel.getFilter().observe(this, filter -> {
            if (filter == JournalViewModel.Filter.ALL && chipGroup.getCheckedChipId() != R.id.journal_chip_all) {
                chipGroup.check(R.id.journal_chip_all);
            } else if (filter == JournalViewModel.Filter.ACCEPTED && chipGroup.getCheckedChipId() != R.id.journal_chip_accepted) {
                chipGroup.check(R.id.journal_chip_accepted);
            } else if (filter == JournalViewModel.Filter.REDRAWN && chipGroup.getCheckedChipId() != R.id.journal_chip_redrawn) {
                chipGroup.check(R.id.journal_chip_redrawn);
            }
        });

        viewModel.getEntries().observe(this, this::updateEntriesUI);

        viewModel.getTotalCount().observe(this, total -> {
            boolean hasTotal = total != null && total > 0;
            btnClearAll.setEnabled(hasTotal);
            // Re-evaluate empty text in case total count changed
            updateEmptyStateText();
        });
    }

    private void updateEntriesUI(@Nullable List<HistoryEntry> entries) {
        boolean empty = entries == null || entries.isEmpty();
        adapter.submitList(entries);

        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            updateEmptyStateText();
        }
    }

    private void updateEmptyStateText() {
        Integer total = viewModel.getTotalCount().getValue();
        if (total == null || total == 0) {
            emptyText.setText(R.string.journal_msg_empty);
        } else {
            emptyText.setText(R.string.journal_msg_empty_filter);
        }
    }

    private void confirmClearAll() {
        Integer total = viewModel.getTotalCount().getValue();
        int count = total != null ? total : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.journal_title_clear_confirm)
                .setMessage(getString(R.string.journal_msg_clear_confirm, count))
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.journal_btn_clear_confirm, (dialog, which) -> viewModel.clearAll())
                .show();
    }
}
