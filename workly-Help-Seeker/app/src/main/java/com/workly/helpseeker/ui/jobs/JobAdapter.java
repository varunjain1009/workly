package com.workly.helpseeker.ui.jobs;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.databinding.ItemJobBinding;

import java.util.ArrayList;
import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private List<Job> jobs = new ArrayList<>();
    private final OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    public JobAdapter(OnJobClickListener listener) {
        this.listener = listener;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = new ArrayList<>();
        if (jobs != null) {
            for (Job job : jobs) {
                this.jobs.add(job);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        holder.bind(jobs.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return jobs.size();
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
