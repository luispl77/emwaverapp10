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

import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emwaver10.Constants;
import com.example.emwaver10.databinding.FragmentReceiveBinding;

import java.util.Arrays;

public class ReceiveFragment extends Fragment {

    private FragmentReceiveBinding binding;

    private PacketModeViewModel packetModeViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(requireActivity()).get(PacketModeViewModel.class);


        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textReceive;
        packetModeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    //textView.setText("click");
                    byte[] command = {'<', 0x22, 3}; // Replace with your actual command
                    byte[] response = sendCommandAndGetResponse(command, 3, 1, 1000);
                    if (response != null) {
                        Log.i("Command Response", Arrays.toString(response));
                        updateTableWithResponse(response);
                        // Run the UI update on the main thread
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                textView.setText("Bytes: " + Arrays.toString(response));
                            });
                        }
                    }

                }).start();
            }
        });

        // Initialize and populate the table
        initializeAndPopulateTable();

        return root;
    }

    // send the command to the service and wait for the broadcast receiver to pick up the response and put in the Queue.
    // this function should not be used in main thread since it can block it.
    private byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
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




}