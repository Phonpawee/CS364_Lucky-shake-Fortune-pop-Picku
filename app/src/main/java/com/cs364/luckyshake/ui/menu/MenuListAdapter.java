package com.cs364.luckyshake.ui.menu;

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
import com.cs364.luckyshake.data.Menu;

import java.util.Objects;

/** Shows the dishes of one menu set. Uses DiffUtil so changes animate and refresh quickly. */
public class MenuListAdapter extends ListAdapter<Menu, MenuListAdapter.ViewHolder> {

    /** Called when the user taps a dish or its delete button. */
    public interface Listener {
        void onEdit(Menu menu);

        void onDelete(Menu menu);
    }

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
    private final Listener listener;

    /**
     * @param thai true to show Thai names, false to show English names
     */
    public MenuListAdapter(boolean thai, Listener listener) {
        super(DIFF);
        this.thai = thai;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Menu menu = getItem(position);
        Context context = holder.itemView.getContext();
        String name = menu.getName(thai);

        holder.name.setText(name);
        holder.meta.setText(MenuLabels.typeAndTags(context, menu));
        holder.delete.setContentDescription(context.getString(R.string.menu_list_label_delete_item, name));

        holder.itemView.setOnClickListener(v -> listener.onEdit(menu));
        holder.delete.setOnClickListener(v -> listener.onDelete(menu));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;
        final ImageButton delete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_menu_name);
            meta = itemView.findViewById(R.id.item_menu_meta);
            delete = itemView.findViewById(R.id.item_menu_delete);
        }
    }
}
