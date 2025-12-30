package com.workly.helpseeker.ui.workers;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.workly.helpseeker.data.model.Worker;
import com.workly.helpseeker.databinding.ItemWorkerBinding;

import java.util.ArrayList;
import java.util.List;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

    private List<Worker> workers = new ArrayList<>();
    private final OnWorkerClickListener listener;

    public interface OnWorkerClickListener {
        void onWorkerSelected(Worker worker);
    }

    public WorkerAdapter(OnWorkerClickListener listener) {
        this.listener = listener;
    }

    public void setWorkers(List<Worker> workers) {
        this.workers = workers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkerBinding binding = ItemWorkerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new WorkerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        holder.bind(workers.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return workers.size();
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
