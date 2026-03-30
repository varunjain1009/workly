package com.workly.helpprovider.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.data.model.JobStatus;
import com.workly.helpprovider.databinding.ItemJobBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JobAdapter extends ListAdapter<Job, JobAdapter.JobViewHolder> {

    private final OnJobClickListener listener;

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
                    && oldItem.getStatus() == newItem.getStatus()
                    && oldItem.getPreferredDateTime() == newItem.getPreferredDateTime();
        }
    };

    public JobAdapter(OnJobClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        private final ItemJobBinding binding;

        public JobViewHolder(ItemJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Job job, OnJobClickListener listener) {
            binding.tvJobTitle.setText(job.getTitle());
            binding.tvJobSkill.setText("Skill: " + job.getRequiredSkill());
            binding.tvJobStatus.setText("Status: " + job.getStatus());

            if (job.getPreferredDateTime() > 0 && job.getStatus() == JobStatus.SCHEDULED) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                binding.tvJobTime.setText("Scheduled: " + sdf.format(new Date(job.getPreferredDateTime())));
                binding.tvJobTime.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvJobTime.setVisibility(android.view.View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onJobClick(job));
        }
    }
}
