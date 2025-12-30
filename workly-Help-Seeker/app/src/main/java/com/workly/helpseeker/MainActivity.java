package com.workly.helpseeker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.workly.helpseeker.databinding.ActivityMainBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Navigation setup will go here once fragments are created
    }
}
