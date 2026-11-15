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

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering vote options summary list in group proposal screen (#37).
 */
public class GroupTallyAdapter extends RecyclerView.Adapter<GroupTallyAdapter.ViewHolder> {

    private final Context context;
    private final ProposalMethod proposalMethod;
    private final FortuneProvider fortuneProvider;
    private final List<VoteOption> items = new ArrayList<>();

    public GroupTallyAdapter(Context context, ProposalMethod proposalMethod) {
        this.context = context;
        this.proposalMethod = proposalMethod;
        this.fortuneProvider = new FortuneProvider(context);
    }

    public void submitList(List<VoteOption> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_tally, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoteOption option = items.get(position);

        String displayName = option.label;
        if (proposalMethod == ProposalMethod.FOOD_TYPE) {
            String translated = fortuneProvider.getFoodTypeName(option.label);
            if (translated != null && !translated.isEmpty()) {
                displayName = translated;
            }
        }
        holder.textName.setText(displayName);

        holder.textVotes.setText(context.getString(R.string.group_tally_votes, option.votes));

        StringBuilder proposersBuilder = new StringBuilder();
        for (int i = 0; i < option.proposers.size(); i++) {
            if (i > 0) {
                proposersBuilder.append(", ");
            }
            int personNum = option.proposers.get(i);
            proposersBuilder.append(context.getString(R.string.group_tally_person, personNum));
        }

        holder.textProposers.setText(context.getString(R.string.group_tally_proposers, proposersBuilder.toString()));

        FontSizeHelper.apply(context, holder.itemView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textProposers;
        final TextView textVotes;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_tally_name);
            textProposers = itemView.findViewById(R.id.text_tally_proposers);
            textVotes = itemView.findViewById(R.id.text_tally_votes);
        }
    }
}
