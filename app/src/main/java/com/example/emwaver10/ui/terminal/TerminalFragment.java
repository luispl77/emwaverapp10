package com.example.emwaver10.ui.terminal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
    private UsbSerialPort finalPort = null;
    private SerialInputOutputManager ioManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textOutput = binding.textOutput;
        editTextInput = binding.editTextInput;

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
                terminalViewModel.sendDataToMainActivity(userInput);
                terminalViewModel.appendData(userInput);
                editTextInput.setText("");
            }
            return false;
        });



        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }






}
