package com.example.emwaver10.ui.packetmode;

import android.os.Bundle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.emwaver10.CC1101;
import com.example.emwaver10.CommandSender;
import com.example.emwaver10.Constants;
import com.example.emwaver10.R;
import com.example.emwaver10.databinding.FragmentPacketModeBinding;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Queue;
import java.util.Arrays;

public class PacketModeFragment extends Fragment implements CommandSender {

    private FragmentPacketModeBinding binding;

    private PacketModeViewModel packetModeViewModel;

    private CC1101 cc;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(this).get(PacketModeViewModel.class);

        cc = new CC1101(this);

        binding = FragmentPacketModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();



        binding.sendTesla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    byte [] teslaSignal = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xCB, (byte)0x8A, 50, -52, -52, -53, 77, 45, 74, -45, 76, -85, 75, 21, -106, 101, -103, -103, -106, -102, 90, -107, -90, -103, 86, -106, 43, 44, -53, 51, 51, 45, 52, -75, 43, 77, 50, -83, 40};
                    cc.sendData(teslaSignal, teslaSignal.length, 300);
                    showToastOnUiThread("tesla signal sent");
                }).start();
            }
        });

        binding.manchesterSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = binding.manchesterSwitch.isChecked();
                // isChecked will be true if the switch is currently to the right (Manchester)
                new Thread(() -> {
                    if(cc.setManchesterEncoding(isChecked)) {
                        showToastOnUiThread("Manchester encoding set successfully to " + isChecked);
                    } else {
                        showToastOnUiThread("Failed to set encoding");
                        // Revert the switch to its previous state on failure
                        // Must run on UI thread as it modifies the view
                        getActivity().runOnUiThread(() -> binding.manchesterSwitch.setChecked(!isChecked));
                    }
                }).start();
            }
        });

        binding.datarateTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String dataRateStr = binding.datarateTextInput.getText().toString();
                    // Parse the string to an integer
                    try {
                        int dataRate = Integer.parseInt(dataRateStr);

                        // Now use dataRate to set the data rate
                        if (cc.setDataRate(dataRate)) {
                            showToastOnUiThread("Data rate set to " + dataRate + " successfully");
                        } else {
                            showToastOnUiThread("Error setting data rate");
                        }
                    } catch (NumberFormatException e) {
                        showToastOnUiThread("Invalid data rate entered");
                    }
                }).start();
            }
            return false;
        });

        binding.initTransmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInit();
                    showToastOnUiThread("check terminal");
                }).start();
            }
        });

        binding.initReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInitRx();
                    showToastOnUiThread("check terminal");
                }).start();
            }
        });

        // Set an OnClickListener for the button
        binding.sendPayloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String payload = binding.payloadDataTextInput.getText().toString();
                Log.i("Payload", payload);
                byte [] payload_bytes = convertHexStringToByteArray(payload);

                new Thread(() -> {
                    cc.sendData(payload_bytes, payload_bytes.length, 300);
                }).start();
            }
        });

        binding.receivePayloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    byte [] receivedBytes = cc.receiveData();
                    if(receivedBytes == null){
                        showToastOnUiThread("no data in fifo");
                        return;
                    }
                    Log.i("Received", cc.toHexStringWithHexPrefix(receivedBytes));
                    updatePayloadText(receivedBytes);
                }).start();
            }
        });

        String[] modulations = getResources().getStringArray(R.array.modulations);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, modulations);
        binding.modulationSelector.setAdapter(arrayAdapter);
        binding.modulationSelector.setOnClickListener(v -> binding.modulationSelector.showDropDown());

        binding.modulationSelector.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item
            String selectedItem = (String) parent.getItemAtPosition(position);
            new Thread(() -> {
                // Handle the selection
                if ("ASK".equals(selectedItem)) {
                    if(cc.setModulation(CC1101.MOD_ASK)){
                        showToastOnUiThread("modulation set successfully to ASK");
                    }
                    else
                        showToastOnUiThread("Failed to set modulation");
                } else if ("FSK".equals(selectedItem)) {
                    if(cc.setModulation(CC1101.MOD_2FSK)){
                        showToastOnUiThread("modulation set successfully to 2FSK");
                    }
                    else
                        showToastOnUiThread("Failed to set modulation");
                }
            }).start();
        });


        return root;
    }

    private void updatePayloadText(byte[] receivedBytes) {
        String hexString = cc.bytesToHexString(receivedBytes);
        // Assuming you are using view binding and the name of your binding class is YourActivityBinding
        getActivity().runOnUiThread(() ->
            binding.payloadDataTextInput.setText(hexString));
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
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers

        IntentFilter filterBytes = new IntentFilter(Constants.ACTION_USB_DATA_BYTES_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filterBytes);
        Log.i("onStart", "receiver registered");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        //Log.i("onDestroy", "on start");
    }

    @Override
    public void onStop() {
        super.onStop();
        requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
        Log.i("onStop", "receiver unregistered");
    }



    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
                        packetModeViewModel.addResponseByte(b);
                    }
                }
            }
        }
    };

    public byte[] convertHexStringToByteArray(String hexString) {
        // Remove any non-hex characters (like spaces) if present
        hexString = hexString.replaceAll("[^0-9A-Fa-f]", "");
        Log.i("Hex Conversion", hexString);

        // Check if the string has an even number of characters
        if (hexString.length() % 2 != 0) {
            Log.e("Hex Conversion", "Invalid hex string");
            return null; // Return null or throw an exception as appropriate
        }

        byte[] bytes = new byte[hexString.length() / 2];

        StringBuilder hex_string = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hexString.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
            hex_string.append(String.format("%02X ", bytes[i]));
        }

        Log.i("Payload bytes", hex_string.toString());

        return bytes;
    }


    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
        // Send the command
        sendByteDataToService(command);

        long startTime = System.currentTimeMillis(); // Start time for timeout

        // Wait for the response with timeout
        while (packetModeViewModel.getResponseQueueSize() < expectedResponseSize) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                Log.e("sendCmdGetResponse", "Timeout occurred");
                //Toast.makeText(getContext(), "timeout", Toast.LENGTH_SHORT).show();
                return null; // Timeout occurred
            }
            try {
                Thread.sleep(busyDelay); // Wait for it to arrive
                //todo: try using wait/notify mechanism to really avoid busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                return null; // Return or handle the interruption as appropriate
            }
        }
        // Retrieve the response
        return packetModeViewModel.getAndClearResponse(expectedResponseSize);
    }

    private void sendByteDataToService(byte[] bytes) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_BYTES_TO_SERVICE);
        intent.putExtra("bytes", bytes);
        requireActivity().sendBroadcast(intent);
    }

    private void sendDataToService(String userInput) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_TO_SERVICE);
        intent.putExtra("userInput", userInput);
        requireActivity().sendBroadcast(intent);
    }


}
