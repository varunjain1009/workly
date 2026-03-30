package com.workly.helpseeker.ui.jobs;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.databinding.ItemJobBinding;

public class JobAdapter extends ListAdapter<Job, JobAdapter.JobViewHolder> {

    private final OnJobClickListener listener;
    private final com.workly.helpseeker.util.AppLogger appLogger;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    private static final DiffUtil.ItemCallback<Job> DIFF_CALLBACK = new DiffUtil.ItemCallback<Job>() {
        @Override
        public boolean areItemsTheSame(@NonNull Job oldItem, @NonNull Job newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Job oldItem, @NonNull Job newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId())
                    && oldItem.getStatus() != null && oldItem.getStatus().equals(newItem.getStatus())
                    && oldItem.getPreferredDateTime() == newItem.getPreferredDateTime();
        }
    };

    public JobAdapter(OnJobClickListener listener, com.workly.helpseeker.util.AppLogger appLogger) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.appLogger = appLogger;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job targetJob = getItem(position);
        appLogger.d("WORKLY_DEBUG", "JobAdapter: UI Binding for position " + position + " | Job ID: " + targetJob.getId());
        holder.bind(targetJob, listener);
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        private final ItemJobBinding binding;

        public JobViewHolder(ItemJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Job job, OnJobClickListener listener) {
            binding.tvJobTitle.setText(job.getTitle());
            binding.tvJobSkill.setText(job.getRequiredSkill());
            binding.tvJobStatus.setText("Status: " + job.getStatus());

            if (job.getPreferredDateTime() > 0
                    && job.getStatus() == com.workly.helpseeker.data.model.JobStatus.SCHEDULED) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm",
                        java.util.Locale.getDefault());
                binding.tvJobTime.setText("Scheduled: " + sdf.format(new java.util.Date(job.getPreferredDateTime())));
                binding.tvJobTime.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvJobTime.setVisibility(android.view.View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onJobClick(job));
        }
    }
}
