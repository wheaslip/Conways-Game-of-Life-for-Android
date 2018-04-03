package com.heslihop.wesley.gameoflife;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.List;

public class SavedStateAdapter extends RecyclerView.Adapter<SavedStateAdapter.MyViewHolder> {

    private List<SavedState> stateList;
    private WeakReference<Context> mContextWeakReference;
    private DatabaseHelper dbh;

    public SavedStateAdapter (List<SavedState> stateList, Context context, DatabaseHelper dbh) {
        this.stateList = stateList;
        this.mContextWeakReference = new WeakReference<Context>(context);
        this.dbh = dbh;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, id, date;
        public RelativeLayout viewForeground, viewBackground;

        public MyViewHolder (View view, final Context context) {
            super(view);
            name = (TextView) view.findViewById(R.id.state_name);
            date = (TextView) view.findViewById(R.id.date);
            viewForeground = (RelativeLayout) view.findViewById(R.id.relative_layout_view_foreground);

            viewForeground.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((SavedStateList) context).userItemClick(getAdapterPosition());
                }
            });

            viewBackground = view.findViewById(R.id.view_background);


        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = mContextWeakReference.get();

        if (context != null) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.saved_state_row, parent, false);

            return new MyViewHolder(itemView, context);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        SavedState state = stateList.get(position);
        holder.name.setText(state.getName());
        holder.date.setText(state.getDate());
    }

    @Override
    public int getItemCount() {
        return stateList.size();
    }

    public void removeItem(int position) {
        dbh.removeEntry(stateList.get(position).getId());
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        stateList.remove(position);
        notifyItemRemoved(position);
    }
}
