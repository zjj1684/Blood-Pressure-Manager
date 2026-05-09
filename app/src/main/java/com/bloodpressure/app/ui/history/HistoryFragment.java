package com.bloodpressure.app.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloodpressure.app.R;
import com.bloodpressure.app.data.db.SessionEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.setSessions(sessions);
        });
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private List<SessionEntity> sessions;
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        void setSessions(List<SessionEntity> sessions) {
            this.sessions = sessions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SessionEntity session = sessions.get(position);
            holder.tvDate.setText(sdf.format(new Date(session.startTime)));
            holder.tvSampleCount.setText(session.sampleCount + " 个采样点");
            holder.tvStatus.setText(session.completed ? "已完成" : "未完成");
        }

        @Override
        public int getItemCount() {
            return sessions != null ? sessions.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvDate;
            final TextView tvSampleCount;
            final TextView tvStatus;

            ViewHolder(View view) {
                super(view);
                tvDate = view.findViewById(R.id.tv_date);
                tvSampleCount = view.findViewById(R.id.tv_sample_count);
                tvStatus = view.findViewById(R.id.tv_status);
            }
        }
    }
}
