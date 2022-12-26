package com.example.step_counter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


//History list adapter to show LiveData step history in History Activity recycler view. Based on Android fundamentals 10 colab.
public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder> {

    private final LayoutInflater mInflater;
    private List<Date> mStepHistory;

    public HistoryListAdapter(Context context){
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.history_recyclerview_item, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if(mStepHistory != null){
            Date current = mStepHistory.get(position);
            holder.dateItemView.setText(current.getDate());
            holder.stepsItemView.setText(Integer.toString(current.getSteps()));
        }else{
            holder.dateItemView.setText("No Date");
            holder.stepsItemView.setText("No Steps");
        }

    }

    void setHistory(List<Date> history){
        mStepHistory = history;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(mStepHistory != null){
            return mStepHistory.size();
        }else  {
            return 0;
        }
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder{
        private final TextView dateItemView;
        private final TextView stepsItemView;

        private HistoryViewHolder(View v){
            super(v);
            dateItemView = v.findViewById(R.id.date_textView);
            stepsItemView = v.findViewById(R.id.steps_textView);
        }
    }
}


