package com.example.emwaver10.ui.packetmode;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emwaver10.CC1101;
import com.example.emwaver10.CommandSender;
import com.example.emwaver10.Constants;
import com.example.emwaver10.databinding.FragmentReceiveBinding;

import java.util.Arrays;

public class ReceiveFragment extends Fragment implements CommandSender {

    private FragmentReceiveBinding binding;

    private PacketModeViewModel packetModeViewModel;

    private CC1101 cc;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(requireActivity()).get(PacketModeViewModel.class);

        cc = new CC1101(this);

        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textReceive;
        packetModeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.receiveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    byte [] receivedBytes = cc.receiveData();
                    Log.i("Received", cc.toHexStringWithHexPrefix(receivedBytes));
                    updateTableWithResponse(receivedBytes);
                }).start();
            }
        });

        binding.receiveInitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInitRx();
                }).start();
            }
        });

        binding.preambleTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String numberString = binding.preambleTextInput.getText().toString();
                int num = Integer.parseInt(numberString);
                Log.i("Preamble", numberString + ": " + num);
                new Thread(() -> {
                    if(cc.setNumPreambleBytes(num)){
                        showToastOnUiThread("Num of preable bytes set successfully: " + num);
                    }
                    else{
                        showToastOnUiThread("Error setting number of preamble bytes: " + num);
                    }

                }).start();
            }
            return false;
        });

        // Initialize and populate the table
        initializeAndPopulateTable();

        return root;
    }

    // send the command to the service and wait for the broadcast receiver to pick up the response and put in the Queue.
    // this function should not be used in main thread since it can block it.
    @Override
    public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
        // Send the command
        sendByteDataToService(command);

        long startTime = System.currentTimeMillis(); // Start time for timeout
        Log.i("Queue Size", ""+packetModeViewModel.getResponseQueueSize());
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

    @Override
    public void onResume() {
        super.onResume();
        // Register BroadcastReceiver here
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers

        IntentFilter filterBytes = new IntentFilter(Constants.ACTION_USB_DATA_BYTES_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filterBytes);
        Log.i("Broadcast", "onResume");
    }

    @Override
    public void onPause() {
        // Unregister BroadcastReceiver here
        super.onPause();
        requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
        Log.i("Broadcast", "broadcast receiver unregistered");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // Fragment is no longer visible
            requireActivity().unregisterReceiver(usbDataReceiver);
            Log.i("Broadcast", "broadcast receiver unregistered");
        } else {
            // Fragment is visible
        }
    }



    private void initializeAndPopulateTable() {
        TableLayout table = binding.tableLayout;

        byte[] data = new byte[64];
        for (int i = 0; i < 64; i++) {
            data[i] = (byte) i;
        }

        int columns = 8; // Number of columns in the table

        for (int i = 0; i < data.length; i++) {
            if (i % columns == 0) {
                TableRow row = new TableRow(getContext());
                table.addView(row);
            }

            TextView textView = new TextView(getContext());
            textView.setText(String.format("%02X", data[i])); // Format byte as Hex
            textView.setPadding(5, 5, 5, 5);
            // Additional formatting

            TableRow currentRow = (TableRow) table.getChildAt(table.getChildCount() - 1);
            currentRow.addView(textView);
        }
    }

    private void updateTableWithResponse(byte[] responseData) {
        // Run on the UI thread
        getActivity().runOnUiThread(() -> {
            TableLayout table = binding.tableLayout;
            table.removeAllViews(); // Clear existing views if necessary

            int columns = 8; // Number of columns in the table
            for (int i = 0; i < responseData.length; i++) {
                if (i % columns == 0) {
                    TableRow row = new TableRow(getContext());
                    table.addView(row);
                }

                TextView textView = new TextView(getContext());
                textView.setText(String.format("%02X", responseData[i])); // Format byte as Hex
                textView.setPadding(5, 5, 5, 5);

                TableRow currentRow = (TableRow) table.getChildAt(table.getChildCount() - 1);
                currentRow.addView(textView);
            }
        });
    }

    public byte[] convertHexStringToByteArray(String hexString) {
        // Remove any non-hex characters (like spaces) if present
        hexString = hexString.replaceAll("[^0-9A-Fa-f]", "");

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




}