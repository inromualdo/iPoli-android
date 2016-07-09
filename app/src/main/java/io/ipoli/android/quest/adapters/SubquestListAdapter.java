package io.ipoli.android.quest.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.squareup.otto.Bus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ipoli.android.R;
import io.ipoli.android.app.events.EventSource;
import io.ipoli.android.app.events.ItemActionsShownEvent;
import io.ipoli.android.quest.data.Subquest;
import io.realm.RealmList;

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 4/28/16.
 */
public class SubquestListAdapter extends RecyclerView.Adapter<SubquestListAdapter.ViewHolder> {
    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    protected Context context;
    protected final Bus evenBus;
    protected List<Subquest> subquests;

    public SubquestListAdapter(Context context, Bus evenBus, List<Subquest> subquests) {
        this.context = context;
        this.evenBus = evenBus;
        this.subquests = subquests;
        viewBinderHelper.setOpenOnlyOne(true);
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.subquest_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Subquest sq = subquests.get(holder.getAdapterPosition());

        viewBinderHelper.bind(holder.swipeLayout, sq.getId());
        holder.swipeLayout.close(false);

        holder.swipeLayout.setSwipeListener(new SwipeRevealLayout.SimpleSwipeListener() {
            @Override
            public void onOpened(SwipeRevealLayout view) {
                super.onOpened(view);
                evenBus.post(new ItemActionsShownEvent(EventSource.SUBQUESTS));
            }
        });


        holder.deleteSubquest.setOnClickListener(iv -> removeSubquest(holder.getAdapterPosition()));

        holder.name.setText(sq.getName());
        holder.check.setOnCheckedChangeListener(null);
        holder.check.setChecked(sq.isCompleted());
        if (sq.isCompleted()) {
            holder.check.setEnabled(false);
        } else {
            holder.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    sq.setCompleted(true);
                } else {
                    sq.setCompleted(false);
                }
            });

            hideUnderline(holder.name);
            holder.name.setOnFocusChangeListener((view, isFocused) -> {
                if (isFocused) {
                    showUnderline(holder.name);
                    holder.name.requestFocus();
                } else {
                    hideUnderline(holder.name);
                }
            });
        }
    }

    private void removeSubquest(int position) {
        subquests.remove(position);
        notifyItemRemoved(position);
    }

    private void showUnderline(TextInputEditText editText) {
        editText.getBackground().clearColorFilter();
    }

    private void hideUnderline(TextInputEditText editText) {
        editText.getBackground().setColorFilter(ContextCompat.getColor(context, android.R.color.transparent), PorterDuff.Mode.SRC_IN);
    }

    @Override
    public int getItemCount() {
        return subquests.size();
    }

    public void addSubquest(Subquest subquest) {
        subquests.add(subquest);
        notifyItemInserted(subquests.size() - 1);
    }

    public void setSubquests(RealmList<Subquest> subquests) {
        this.subquests.clear();
        this.subquests.addAll(subquests);
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.subquest_check)
        CheckBox check;

        @BindView(R.id.subquest_name)
        TextInputEditText name;

        @BindView(R.id.swipe_layout)
        public SwipeRevealLayout swipeLayout;

        @BindView(R.id.delete_subquest)
        public ImageButton deleteSubquest;

        public ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }
}
