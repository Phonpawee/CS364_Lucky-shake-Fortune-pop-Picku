package com.cs364.luckyshake.ui.weights;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Objects;

/**
 * Adapter for rendering menu adaptive weights list in the "How the app thinks" screen (#42).
 */
public class MenuWeightsAdapter extends ListAdapter<Menu, MenuWeightsAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<Menu> DIFF = new DiffUtil.ItemCallback<Menu>() {
        @Override
        public boolean areItemsTheSame(@NonNull Menu a, @NonNull Menu b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Menu a, @NonNull Menu b) {
            return a.setId == b.setId
                    && Objects.equals(a.nameTh, b.nameTh)
                    && Objects.equals(a.nameEn, b.nameEn)
                    && Objects.equals(a.type, b.type)
                    && a.tags == b.tags
                    && a.weight == b.weight;
        }
    };

    private final boolean thai;

    public MenuWeightsAdapter(boolean thai) {
        super(DIFF);
        this.thai = thai;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_weight, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Menu menu = getItem(position);
        Context context = holder.itemView.getContext();
        String name = menu.getName(thai);

        int progress = WeightLabels.toProgress(menu.weight);
        int cat = WeightLabels.category(menu.weight);

        holder.name.setText(name);
        holder.progress.setProgress(progress);
        holder.value.setText(context.getString(R.string.weights_value, progress, GameConfig.MAX_WEIGHT));

        if (cat == WeightLabels.LIKED) {
            holder.category.setText(R.string.weights_label_liked);
        } else if (cat == WeightLabels.DISLIKED) {
            holder.category.setText(R.string.weights_label_disliked);
        } else {
            holder.category.setText(R.string.weights_label_neutral);
        }

        String barDesc = context.getString(R.string.weights_bar_description, name, progress, GameConfig.MAX_WEIGHT);
        holder.progress.setContentDescription(barDesc);
        holder.itemView.setContentDescription(barDesc);

        FontSizeHelper.apply(context, holder.itemView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final LinearProgressIndicator progress;
        final TextView value;
        final TextView category;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_weight_name);
            progress = itemView.findViewById(R.id.item_weight_progress);
            value = itemView.findViewById(R.id.item_weight_value);
            category = itemView.findViewById(R.id.item_weight_category);
        }
    }
}
