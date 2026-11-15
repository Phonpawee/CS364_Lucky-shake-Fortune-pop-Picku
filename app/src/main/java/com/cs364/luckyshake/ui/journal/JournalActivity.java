package com.cs364.luckyshake.ui.journal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.data.HistoryRepository;
import com.cs364.luckyshake.ui.nav.BottomNav;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen displaying the user's fortune draw history and statistics (#40, #41).
 * Supports filtering entries, deleting individual items, clearing all entries,
 * rendering stats charts, and handling empty state messages.
 */
public class JournalActivity extends AppCompatActivity {

    private JournalViewModel viewModel;
    private View root;
    private MaterialButtonToggleGroup toggleGroupTab;
    private View journalFilterRow;
    private View journalCardEntries;
    private View viewJournalStats;
    private ChipGroup chipGroup;
    private MaterialButton btnClearAll;
    private RecyclerView recycler;
    private JournalAdapter adapter;
    private View emptyGroup;
    private TextView emptyText;

    // Stats card views
    private View cardStatsTop;
    private View cardStatsRatio;
    private View cardStatsWeek;
    private TextView statsEmptyText;
    private LinearLayout topDishesContainer;
    private ProgressBar progressRatio;
    private TextView labelRatio;

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
        BottomNav.attach(this, R.id.nav_journal);

        Locale locale = getResources().getConfiguration().getLocales().get(0);
        boolean thai = "th".equals(locale.getLanguage());

        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);

        toggleGroupTab = findViewById(R.id.journal_toggle_group_tab);
        journalFilterRow = findViewById(R.id.journal_filter_row);
        journalCardEntries = findViewById(R.id.journal_card_entries);
        viewJournalStats = findViewById(R.id.view_journal_stats);

        chipGroup = findViewById(R.id.journal_chip_group);
        btnClearAll = findViewById(R.id.journal_btn_clear_all);
        recycler = findViewById(R.id.journal_recycler);
        emptyGroup = findViewById(R.id.journal_empty_group);
        emptyText = findViewById(R.id.journal_empty_text);

        // Stats card views inside view_journal_stats
        if (viewJournalStats != null) {
            cardStatsTop = viewJournalStats.findViewById(R.id.card_stats_top);
            cardStatsRatio = viewJournalStats.findViewById(R.id.card_stats_ratio);
            cardStatsWeek = viewJournalStats.findViewById(R.id.card_stats_week);
            statsEmptyText = viewJournalStats.findViewById(R.id.stats_empty_text);
            topDishesContainer = viewJournalStats.findViewById(R.id.stats_top_dishes_container);
            progressRatio = viewJournalStats.findViewById(R.id.stats_progress_ratio);
            labelRatio = viewJournalStats.findViewById(R.id.stats_label_ratio);
        }

        adapter = new JournalAdapter(this, thai, locale, entry -> {
            viewModel.delete(entry.id);
            BottomNav.show(this, Snackbar.make(root, R.string.journal_msg_deleted,
                    Snackbar.LENGTH_SHORT));
        });

        if (recycler != null) {
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
        }

        if (toggleGroupTab != null) {
            toggleGroupTab.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.journal_btn_tab_entries) {
                        viewModel.setSelectedTab(JournalViewModel.Tab.ENTRIES);
                    } else if (checkedId == R.id.journal_btn_tab_stats) {
                        viewModel.setSelectedTab(JournalViewModel.Tab.STATS);
                    }
                }
            });
        }

        if (chipGroup != null) {
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
        }

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> confirmClearAll());
        }

        observeViewModel(thai, locale);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontSizeHelper.apply(this, root);   // the user may have changed the size in Settings
        viewModel.refresh();
    }

    private void observeViewModel(boolean thai, Locale locale) {
        viewModel.getSelectedTab().observe(this, tab -> {
            if (tab == JournalViewModel.Tab.STATS) {
                if (toggleGroupTab != null && toggleGroupTab.getCheckedButtonId() != R.id.journal_btn_tab_stats) {
                    toggleGroupTab.check(R.id.journal_btn_tab_stats);
                }
                if (journalFilterRow != null) journalFilterRow.setVisibility(View.GONE);
                if (journalCardEntries != null) journalCardEntries.setVisibility(View.GONE);
                if (viewJournalStats != null) viewJournalStats.setVisibility(View.VISIBLE);
            } else {
                if (toggleGroupTab != null && toggleGroupTab.getCheckedButtonId() != R.id.journal_btn_tab_entries) {
                    toggleGroupTab.check(R.id.journal_btn_tab_entries);
                }
                if (journalFilterRow != null) journalFilterRow.setVisibility(View.VISIBLE);
                if (journalCardEntries != null) journalCardEntries.setVisibility(View.VISIBLE);
                if (viewJournalStats != null) viewJournalStats.setVisibility(View.GONE);
            }
        });

        viewModel.getFilter().observe(this, filter -> {
            if (chipGroup == null) return;
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
            if (btnClearAll != null) {
                btnClearAll.setEnabled(hasTotal);
            }
            updateEmptyStateText();
        });

        viewModel.getStats().observe(this, stats -> updateStatsUI(stats, thai, locale));
    }

    private void updateEntriesUI(@Nullable List<HistoryEntry> entries) {
        boolean empty = entries == null || entries.isEmpty();
        adapter.submitList(entries);

        if (recycler != null) recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyGroup != null) emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            updateEmptyStateText();
        }
    }

    private void updateEmptyStateText() {
        if (emptyText == null) return;
        Integer total = viewModel.getTotalCount().getValue();
        if (total == null || total == 0) {
            emptyText.setText(R.string.journal_msg_empty);
        } else {
            emptyText.setText(R.string.journal_msg_empty_filter);
        }
    }

    private void updateStatsUI(@Nullable JournalStats stats, boolean thai, Locale locale) {
        if (viewJournalStats == null) return;

        if (stats == null || stats.isEmpty()) {
            if (cardStatsTop != null) cardStatsTop.setVisibility(View.GONE);
            if (cardStatsRatio != null) cardStatsRatio.setVisibility(View.GONE);
            if (cardStatsWeek != null) cardStatsWeek.setVisibility(View.GONE);
            if (statsEmptyText != null) {
                statsEmptyText.setText(R.string.journal_msg_empty);
                statsEmptyText.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (cardStatsTop != null) cardStatsTop.setVisibility(View.VISIBLE);
        if (cardStatsRatio != null) cardStatsRatio.setVisibility(View.VISIBLE);
        if (cardStatsWeek != null) cardStatsWeek.setVisibility(View.VISIBLE);
        if (statsEmptyText != null) statsEmptyText.setVisibility(View.GONE);

        // 1. Render Top Dishes
        if (topDishesContainer != null) {
            topDishesContainer.removeAllViews();
            int maxTop = stats.maxTopCount();
            LayoutInflater inflater = LayoutInflater.from(this);

            for (HistoryRepository.NameCount nc : stats.topDishes) {
                View row = inflater.inflate(R.layout.item_stats_top_dish, topDishesContainer, false);
                TextView nameView = row.findViewById(R.id.stats_top_dish_name);
                ProgressBar progView = row.findViewById(R.id.stats_top_dish_progress);
                TextView countView = row.findViewById(R.id.stats_top_dish_count);

                nameView.setText(nc.getName(thai));
                int progressPercent = (nc.count * 100) / maxTop;
                progView.setProgress(progressPercent);
                countView.setText(String.valueOf(nc.count));

                topDishesContainer.addView(row);
            }
        }

        // 2. Render Ratio
        if (progressRatio != null) {
            progressRatio.setProgress(stats.acceptPercent());
        }
        if (labelRatio != null) {
            labelRatio.setText(getString(R.string.stats_label_ratio,
                    stats.accepted, stats.redrawn, stats.acceptPercent()));
        }

        // 3. Render 7 Days Bar Chart
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat daySdf = new SimpleDateFormat(thai ? "EEEE" : "EEE", locale);
        int maxDay = stats.maxPerDay();

        int[] countIds = {
                R.id.stats_bar_count_0, R.id.stats_bar_count_1, R.id.stats_bar_count_2,
                R.id.stats_bar_count_3, R.id.stats_bar_count_4, R.id.stats_bar_count_5,
                R.id.stats_bar_count_6
        };
        int[] viewIds = {
                R.id.stats_bar_view_0, R.id.stats_bar_view_1, R.id.stats_bar_view_2,
                R.id.stats_bar_view_3, R.id.stats_bar_view_4, R.id.stats_bar_view_5,
                R.id.stats_bar_view_6
        };
        int[] dayIds = {
                R.id.stats_bar_day_0, R.id.stats_bar_day_1, R.id.stats_bar_day_2,
                R.id.stats_bar_day_3, R.id.stats_bar_day_4, R.id.stats_bar_day_5,
                R.id.stats_bar_day_6
        };

        for (int i = 0; i < 7; i++) {
            TextView countView = viewJournalStats.findViewById(countIds[i]);
            View barView = viewJournalStats.findViewById(viewIds[i]);
            TextView dayView = viewJournalStats.findViewById(dayIds[i]);

            if (countView != null && barView != null && dayView != null) {
                int cnt = stats.perDay[i];
                countView.setText(String.valueOf(cnt));

                String dayName;
                if (stats.perDayMillis != null && stats.perDayMillis.length > i && stats.perDayMillis[i] > 0) {
                    dayName = daySdf.format(new Date(stats.perDayMillis[i]));
                } else {
                    dayName = daySdf.format(cal.getTime());
                }
                dayView.setText(dayName);

                int heightDp = cnt == 0 ? 3 : Math.max(3, (cnt * 100) / maxDay);
                int heightPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, heightDp, getResources().getDisplayMetrics());

                ViewGroup.LayoutParams lp = barView.getLayoutParams();
                if (lp != null) {
                    lp.height = heightPx;
                    barView.setLayoutParams(lp);
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
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
