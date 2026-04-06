package com.workly.helpprovider.ui.jobs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textview.MaterialTextView;

import com.workly.helpprovider.R;
import com.workly.helpprovider.data.model.Job;
import com.workly.helpprovider.ui.adapter.JobAdapter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyJobsFragment extends Fragment implements JobAdapter.OnJobClickListener {

    private static final String TAG = "WORKLY_DEBUG";

    private MyJobsViewModel viewModel;
    private JobAdapter jobAdapter;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvJobs;
    private MaterialTextView tvEmpty;
    private String currentType = "active";

    private final android.content.BroadcastReceiver jobsUpdatedReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if ("PROVIDER_MY_JOBS_UPDATED".equals(intent.getAction())) {
                viewModel.loadMyJobs(currentType);
            }
        }
    };

    @Inject
    com.workly.helpprovider.util.AppLogger appLogger;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_jobs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_my_jobs);
        rvJobs = view.findViewById(R.id.rv_my_jobs);
        tvEmpty = view.findViewById(R.id.tv_my_jobs_empty);

        viewModel = new ViewModelProvider(requireActivity()).get(MyJobsViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupTabs(view);

        swipeRefresh.setOnRefreshListener(() -> {
            appLogger.d(TAG, "MyJobsFragment(Provider): Manual refresh triggered");
            viewModel.loadMyJobs(currentType);
        });
    }

    private void setupTabs(@NonNull View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_my_job_type);
        updateEmptyText();
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            currentType = checkedId == R.id.btn_completed_jobs ? "completed" : "active";
            updateEmptyText();
            viewModel.loadMyJobs(currentType);
        });
    }

    private void setupRecyclerView() {
        jobAdapter = new JobAdapter(this);
        rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        rvJobs.setAdapter(jobAdapter);
    }

    private void setupObservers() {
        viewModel.getMyJobs().observe(getViewLifecycleOwner(), jobs -> {
            swipeRefresh.setRefreshing(false);
            if (jobs != null && !jobs.isEmpty()) {
                appLogger.d(TAG, "MyJobsFragment(Provider): Showing " + jobs.size() + " jobs");
                jobAdapter.submitList(jobs);
                rvJobs.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                appLogger.d(TAG, "MyJobsFragment(Provider): No jobs found");
                jobAdapter.submitList(null);
                rvJobs.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                updateEmptyText();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                swipeRefresh.setRefreshing(true);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                swipeRefresh.setRefreshing(false);
                Snackbar.make(requireView(), err, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onJobClick(Job job) {
        appLogger.d(TAG, "MyJobsFragment(Provider): Job clicked - ID: " + job.getId());
        Bundle bundle = new Bundle();
        bundle.putSerializable("job", job);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_my_jobs_to_jobDetails, bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(jobsUpdatedReceiver, new android.content.IntentFilter("PROVIDER_MY_JOBS_UPDATED"));
        viewModel.loadMyJobs(currentType);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(jobsUpdatedReceiver);
    }

    private void updateEmptyText() {
        tvEmpty.setText("completed".equals(currentType)
                ? "No completed jobs yet."
                : "No active jobs yet.\nAccept a job to see it here.");
    }
}
