package com.cs364.luckyshake.ui.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.fortune.FortuneProvider;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Objects;

/**
 * Adapter for rendering the 10 food category options in group proposal mode (#35).
 */
public class FoodTypeAdapter extends RecyclerView.Adapter<FoodTypeAdapter.ViewHolder> {

    public interface Listener {
        void onFoodTypeSelected(String key);
    }

    private final Context context;
    private final FortuneProvider fortuneProvider;
    private final Listener listener;
    private final List<String> keys;
    private String selectedKey;

    public FoodTypeAdapter(Context context, Listener listener) {
        this.context = context;
        this.fortuneProvider = new FortuneProvider(context);
        this.listener = listener;
        this.keys = FoodTypes.KEYS;
    }

    public void setSelectedKey(String selectedKey) {
        if (!Objects.equals(this.selectedKey, selectedKey)) {
            this.selectedKey = selectedKey;
            notifyDataSetChanged();
        }
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    @Override
    public int getItemCount() {
        return keys.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_food_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = keys.get(position);
        String displayName = fortuneProvider.getFoodTypeName(key);

        holder.typeNameText.setText(displayName != null ? displayName : key);

        boolean isSelected = Objects.equals(key, selectedKey);
        holder.cardView.setCheckable(true);
        holder.cardView.setChecked(isSelected);
        holder.cardView.setSelected(isSelected);

        String accessibleName = displayName != null ? displayName : key;
        String accessibilityDesc = isSelected
                ? context.getString(
                        R.string.group_setup_label_food_type_selected,
                        accessibleName,
                        context.getString(R.string.group_setup_label_method_food_type))
                : accessibleName;
        holder.cardView.setContentDescription(accessibilityDesc);

        holder.cardView.setOnClickListener(v -> {
            setSelectedKey(key);
            if (listener != null) {
                listener.onFoodTypeSelected(key);
            }
        });

        FontSizeHelper.apply(context, holder.itemView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView typeNameText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            typeNameText = itemView.findViewById(R.id.text_food_type_name);
        }
    }
}
