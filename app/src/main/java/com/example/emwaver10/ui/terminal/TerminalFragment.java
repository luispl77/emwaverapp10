package com.example.emwaver10.ui.terminal;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.emwaver10.Constants;
import com.example.emwaver10.R;
import com.example.emwaver10.databinding.FragmentTerminalBinding;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;


public class TerminalFragment extends Fragment{
    private FragmentTerminalBinding binding;
    private EditText editTextInput;
    private TextView textOutput;
    private TerminalViewModel terminalViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textOutput = binding.textOutput;
        editTextInput = binding.editTextInput;
        // Set the TextView as scrollable
        binding.textOutput.setMovementMethod(new ScrollingMovementMethod());

        terminalViewModel = new ViewModelProvider(this).get(TerminalViewModel.class);
        // Observe the LiveData and update the UI accordingly
        terminalViewModel.getTerminalData().observe(getViewLifecycleOwner(), text -> {
            textOutput.setText(text);
        });

        // Sample: display input from EditText to TextView when the user hits 'Enter'
        editTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = editTextInput.getText().toString();
                // Assuming userInput is the data you want to send to MainActivity
                sendUserInputToService(userInput);
                terminalViewModel.appendData(userInput);
                editTextInput.setText("");
            }
            return false;
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("com.example.ACTION_USB_DATA");
        requireActivity().registerReceiver(usbDataReceiver, filter);
    }

    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.ACTION_USB_DATA".equals(intent.getAction())) {
                String dataString = intent.getStringExtra("data");
                // Update UI or ViewModel with the received data
                updateUI(dataString);
            }
        }
    };

    private void updateUI(String data) {
        // Update your UI elements here
        terminalViewModel.appendData(data);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("ter", "click listened");
                onConnectClick(v);
            }
        });

        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("ter", "click listened clear");
                terminalViewModel.setData("");
                textOutput.setText("");
            }
        });
    }

    public void onConnectClick(View view) {
        Intent intent = new Intent(Constants.ACTION_CONNECT_USB);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        //requireActivity().unregisterReceiver(usbDataReceiver);
        super.onStop();
    }

    private void sendUserInputToService(String userInput) {
        Intent intent = new Intent("com.example.ACTION_SEND_DATA_TO_SERVICE");
        intent.putExtra("userInput", userInput);
        requireActivity().sendBroadcast(intent);
    }
}
