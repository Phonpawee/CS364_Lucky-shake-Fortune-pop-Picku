package com.cs364.luckyshake.ui.journal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;

import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * ListAdapter for rendering fortune draw history entries in the Journal screen.
 * Uses DiffUtil and stable IDs to ensure smooth scrolling performance across hundreds of entries.
 */
public class JournalAdapter extends ListAdapter<HistoryEntry, JournalAdapter.ViewHolder> {

    public interface Listener {
        void onDelete(HistoryEntry entry);
    }

    private static final DiffUtil.ItemCallback<HistoryEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<HistoryEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull HistoryEntry oldItem, @NonNull HistoryEntry newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull HistoryEntry oldItem, @NonNull HistoryEntry newItem) {
                    return oldItem.id == newItem.id
                            && Objects.equals(oldItem.nameTh, newItem.nameTh)
                            && Objects.equals(oldItem.nameEn, newItem.nameEn)
                            && oldItem.level == newItem.level
                            && Objects.equals(oldItem.status, newItem.status)
                            && Objects.equals(oldItem.mode, newItem.mode)
                            && oldItem.createdAt == newItem.createdAt;
                }
            };

    private final Context context;
    private final boolean thai;
    private final Locale locale;
    private final Listener listener;

    private final String filledStar;
    private final String emptyStar;
    private final String statusAcceptedText;
    private final String statusRedrawnText;
    private final String statusAcceptedDesc;
    private final String statusRedrawnDesc;
    private final String modeGroupText;
    private final String modeSoloText;
    private final String deleteDesc;
    private final String level1Text;
    private final String level2Text;
    private final String level3Text;
    private final String level4Text;

    public JournalAdapter(Context context, boolean thai, Locale locale, Listener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.thai = thai;
        this.locale = locale != null ? locale : Locale.getDefault();
        this.listener = listener;
        setHasStableIds(true);

        // Cache static string resources to avoid repeated getString calls during binding
        filledStar = context.getString(R.string.journal_label_star_filled);
        emptyStar = context.getString(R.string.journal_label_star_empty);
        statusAcceptedText = context.getString(R.string.journal_label_status_accepted);
        statusRedrawnText = context.getString(R.string.journal_label_status_redrawn);
        statusAcceptedDesc = context.getString(R.string.journal_label_status_accepted_desc);
        statusRedrawnDesc = context.getString(R.string.journal_label_status_redrawn_desc);
        modeGroupText = context.getString(R.string.journal_label_mode_group);
        modeSoloText = context.getString(R.string.journal_label_mode_solo);
        deleteDesc = context.getString(R.string.journal_btn_delete_desc);
        level1Text = context.getString(R.string.journal_label_level_1);
        level2Text = context.getString(R.string.journal_label_level_2);
        level3Text = context.getString(R.string.journal_label_level_3);
        level4Text = context.getString(R.string.journal_label_level_4);
    }

    @Override
    public long getItemId(int position) {
        HistoryEntry entry = getItem(position);
        return entry != null ? entry.id : RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry item = getItem(position);
        if (item == null) {
            return;
        }

        boolean isAccepted = HistoryEntry.STATUS_ACCEPTED.equals(item.status);
        holder.statusText.setText(isAccepted ? statusAcceptedText : statusRedrawnText);
        holder.statusText.setContentDescription(isAccepted ? statusAcceptedDesc : statusRedrawnDesc);

        holder.nameText.setText(item.getName(thai));

        String timeStr = JournalFormat.formatTime(item.createdAt, locale, TimeZone.getDefault());
        String starStr = JournalFormat.stars(item.level, filledStar, emptyStar);
        String levelStr = getLevelName(item.level);
        String modeStr = HistoryEntry.MODE_GROUP.equals(item.mode) ? modeGroupText : modeSoloText;

        String metaStr = context.getString(R.string.journal_label_meta, timeStr, starStr, levelStr, modeStr);
        holder.metaText.setText(metaStr);

        FontSizeHelper.apply(context, holder.itemView);   // rows are created after the screen

        holder.deleteButton.setContentDescription(deleteDesc);
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
        });
    }

    private String getLevelName(int level) {
        int clamped = JournalFormat.clampLevel(level);
        switch (clamped) {
            case 1:
                return level1Text;
            case 2:
                return level2Text;
            case 3:
                return level3Text;
            case 4:
            default:
                return level4Text;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView statusText;
        final TextView nameText;
        final TextView metaText;
        final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusText = itemView.findViewById(R.id.journal_item_status);
            nameText = itemView.findViewById(R.id.journal_item_name);
            metaText = itemView.findViewById(R.id.journal_item_meta);
            deleteButton = itemView.findViewById(R.id.journal_item_delete);
        }
    }
}
