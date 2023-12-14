package com.example.emwaver10.ui.scripts;

import androidx.lifecycle.ViewModelProvider;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emwaver10.CC1101;
import com.example.emwaver10.Constants;
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
                    ScriptsEngine scriptsEngine = new ScriptsEngine();
                    scriptsEngine.executeJavaScript(jsCode, getContext(), scriptsViewModel);
                }).start();
            }
        });


        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register BroadcastReceiver here
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers

        IntentFilter filterBytes = new IntentFilter(Constants.ACTION_USB_DATA_BYTES_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filterBytes);
    }

    @Override
    public void onStop() {
        super.onStop();
        //requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
        //Log.i("onStop", "receiver unregistered");
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers

        IntentFilter filterBytes = new IntentFilter(Constants.ACTION_USB_DATA_BYTES_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filterBytes);
        //Log.i("onStart", "receiver registered");
    }

    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if (Constants.ACTION_USB_DATA_RECEIVED.equals(intent.getAction())) {
                String dataString = intent.getStringExtra("data");
                if (dataString != null) {
                    //Log.i("ser string", dataString);
                }
            }
            else if (Constants.ACTION_USB_DATA_BYTES_RECEIVED.equals(intent.getAction())) {
                byte [] bytes = intent.getByteArrayExtra("bytes");
                if (bytes != null) {
                    // Optionally, you can log the byte array to see its contents
                    //Log.i("service bytes", Arrays.toString(bytes));
                    for (byte b : bytes) {
                        scriptsViewModel.addResponseByte(b);
                    }
                }
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for avoiding memory leaks
    }
}
