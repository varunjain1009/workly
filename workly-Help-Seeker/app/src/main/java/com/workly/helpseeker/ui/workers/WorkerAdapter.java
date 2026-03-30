package com.workly.helpseeker.ui.workers;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.workly.helpseeker.data.model.Worker;
import com.workly.helpseeker.databinding.ItemWorkerBinding;

public class WorkerAdapter extends ListAdapter<Worker, WorkerAdapter.WorkerViewHolder> {

    private final OnWorkerClickListener listener;
    private final com.workly.helpseeker.util.AppLogger appLogger;

    public interface OnWorkerClickListener {
        void onWorkerSelected(Worker worker);
    }

    private static final DiffUtil.ItemCallback<Worker> DIFF_CALLBACK = new DiffUtil.ItemCallback<Worker>() {
        @Override
        public boolean areItemsTheSame(@NonNull Worker oldItem, @NonNull Worker newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Worker oldItem, @NonNull Worker newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId())
                    && Float.compare(oldItem.getRating(), newItem.getRating()) == 0
                    && oldItem.getReviewCount() == newItem.getReviewCount();
        }
    };

    public WorkerAdapter(OnWorkerClickListener listener, com.workly.helpseeker.util.AppLogger appLogger) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.appLogger = appLogger;
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkerBinding binding = ItemWorkerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new WorkerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Worker targetWorker = getItem(position);
        appLogger.d("WORKLY_DEBUG", "WorkerAdapter: UI Binding viewholder index " + position + " | worker id: " + targetWorker.getId());
        holder.bind(targetWorker, listener);
    }

    static class WorkerViewHolder extends RecyclerView.ViewHolder {
        private final ItemWorkerBinding binding;

        public WorkerViewHolder(ItemWorkerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Worker worker, OnWorkerClickListener listener) {
            binding.tvWorkerName.setText(worker.getName());
            binding.tvWorkerRating.setText(worker.getRating() + " ⭐ (" + worker.getReviewCount() + ")");
            binding.tvSkillMatch.setText("Skill Match: " + (int) (worker.getSkillMatch() * 100) + "%");
            binding.tvDistance.setText("Distance: " + worker.getDistanceKm() + " km");
            binding.tvCharges.setText("Est. Charges: $" + worker.getEstimatedCharges());

            binding.btnHire.setOnClickListener(v -> listener.onWorkerSelected(worker));
            itemView.setOnClickListener(v -> listener.onWorkerSelected(worker));
        }
    }
}
