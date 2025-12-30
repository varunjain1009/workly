package com.workly.helpseeker.ui.workers;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.workly.helpseeker.data.model.Worker;
import com.workly.helpseeker.data.network.ApiResponse;
import com.workly.helpseeker.data.network.ApiService;
import com.workly.helpseeker.databinding.FragmentWorkerDiscoveryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class WorkerDiscoveryFragment extends Fragment {

    private static final String TAG = "WORKLY_DEBUG";
    private FragmentWorkerDiscoveryBinding binding;
    private WorkerAdapter adapter;
    private boolean debugEnabled = false;

    @Inject
    ApiService apiService;

    @Inject
    Properties properties;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerDiscoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        debugEnabled = Boolean.parseBoolean(properties.getProperty("app.debug_enabled", "false"));

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupRecyclerView();

        if (getArguments() != null) {
            String skill = getArguments().getString("skill");
            int radius = getArguments().getInt("radius", 10);
            if (debugEnabled)
                Log.d(TAG, "Searching for: " + skill + " within " + radius + "km");
            searchWorkers(skill, radius);
        } else {
            loadDummyWorkers();
        }
    }

    private void searchWorkers(String skill, int radius) {
        // Using dummy lat/lon for now as per user GPS location requirement (to be
        // implemented)
        apiService.searchWorkers(skill, 0.0, 0.0, radius).enqueue(new Callback<ApiResponse<List<Worker>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Worker>>> call, Response<ApiResponse<List<Worker>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Worker> workers = response.body().getData();
                    if (debugEnabled)
                        Log.d(TAG, "Found " + (workers != null ? workers.size() : 0) + " workers");
                    adapter.setWorkers(workers != null ? workers : new ArrayList<>());
                } else {
                    if (debugEnabled)
                        Log.e(TAG, "Search failed. Status: " + response.code());
                    Toast.makeText(getContext(), "Failed to find workers", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Worker>>> call, Throwable t) {
                if (debugEnabled)
                    Log.e(TAG, "Network error during search: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new WorkerAdapter(worker -> {
            Toast.makeText(getContext(), "Selected: " + worker.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Navigate or perform assignment
        });
        binding.rvWorkers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvWorkers.setAdapter(adapter);
    }

    private void loadDummyWorkers() {
        List<Worker> dummyWorkers = new ArrayList<>();
        // Add dummy data for testing UI
        adapter.setWorkers(dummyWorkers);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
