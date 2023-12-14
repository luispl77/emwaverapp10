package com.example.emwaver10.ui.scripts;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emwaver10.CC1101;
import com.example.emwaver10.R;
import com.example.emwaver10.databinding.FragmentScriptsBinding;

public class ScriptsFragment extends Fragment {

    private ScriptsViewModel scriptsViewModel;
    private FragmentScriptsBinding binding; // Binding class for the fragment_scripts.xml layout


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Initialize view binding
        binding = FragmentScriptsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scriptsViewModel = new ViewModelProvider(this).get(ScriptsViewModel.class);

        // Set up your button click listeners
        binding.executeScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    String jsCode = binding.jsCodeInput.getText().toString(); // Get code from EditText
                    //jsCode = "for (var i = 0; i < 10; i++) { print('Hello from JavaScript! ' + (i + 1));}";
                    Log.i("script", jsCode);
                    ScriptsEngine scriptsEngine = new ScriptsEngine();
                    scriptsEngine.executeJavaScript(jsCode, getContext());
                }).start();
            }
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for avoiding memory leaks
    }
}
