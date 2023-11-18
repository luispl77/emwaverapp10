package com.example.emwaver10.ui.terminal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
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


public class TerminalFragment extends Fragment implements View.OnClickListener, SerialInputOutputManager.Listener{
    private FragmentTerminalBinding binding;
    private EditText editTextInput;
    private TextView textOutput;
    private TerminalViewModel terminalViewModel;
    private UsbSerialPort finalPort = null;
    private SerialInputOutputManager ioManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        terminalViewModel = new ViewModelProvider(this).get(TerminalViewModel.class);

        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textOutput = binding.textOutput;
        editTextInput = binding.editTextInput;

        // Locate the connect button
        Button connectButton = binding.connectButton;
        // Set click listener to the fragment
        connectButton.setOnClickListener(this);

        // Observe the LiveData and update the UI accordingly
        terminalViewModel.getTerminalData().observe(getViewLifecycleOwner(), text -> {
            textOutput.setText(text);
        });

        // Sample: display input from EditText to TextView when the user hits 'Enter'
        editTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = editTextInput.getText().toString();



                // Check if the device is connected and finalPort is available
                if (finalPort != null) {
                    try {
                        // Write the user input to the USB port
                        finalPort.write(userInput.getBytes(), 2000);
                        Toast.makeText(getContext(), "Data sent to USB port.", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Failed to send data to USB port.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getContext(), "USB port not connected.", Toast.LENGTH_SHORT).show();
                }

                // Update the view model with the user input
                terminalViewModel.appendData(userInput);

                // Clear the EditText after processing the input
                editTextInput.setText("");
                return true;
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.connectButton) {
            Toast.makeText(getContext(), "connecting...", Toast.LENGTH_SHORT).show();
            try {
                finalPort = connectUSBAndReturnPort();
                if (finalPort != null) {
                    Toast.makeText(getContext(), "Connected to driver: " + finalPort + " max pkt size: " + finalPort.getReadEndpoint().getMaxPacketSize(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "No port available", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(getContext(), "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onNewData(byte[] data) {
        // Convert the data to a string, assuming UTF-8 encoding.
        String dataString;
        try {
            dataString = new String(data, "UTF-8");
            // Update the view model with the new data.
            terminalViewModel.appendData(dataString);

            // Since Toasts need to be shown on the main thread, use getActivity().runOnUiThread()
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "New data received", Toast.LENGTH_SHORT).show();
                // Update your UI elements here if necessary
                textOutput.setText(dataString);
            });
        } catch (UnsupportedEncodingException e) {
            // Handle the exception if UTF-8 encoding is not supported
            e.printStackTrace();
        }
    }



    @Override
    public void onRunError(Exception e) {

    }


    public UsbSerialPort connectUSBAndReturnPort() throws IOException {
        UsbManager manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Toast.makeText(getContext(), "No devices found", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);

        //USB PERMISSION CODE:
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent("com.example.emwaver10.GRANT_USB"), flags);
        manager.requestPermission(driver.getDevice(), usbPermissionIntent);

        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Toast.makeText(getContext(), "Connection returned null", Toast.LENGTH_SHORT).show();
            return null;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        port.open(connection);
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        ioManager = new SerialInputOutputManager(port, this);
        ioManager.start();

        return port;
    }
}
