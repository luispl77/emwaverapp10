package com.example.emwaver10.ui.packetmode;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.emwaver10.Constants;
import com.example.emwaver10.R;
import com.example.emwaver10.databinding.FragmentReceiveBinding;
import com.example.emwaver10.databinding.FragmentTransmitBinding;

import java.util.Arrays;

public class TransmitFragment extends Fragment {

    private FragmentTransmitBinding binding;

    private PacketModeViewModel packetModeViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        packetModeViewModel = new ViewModelProvider(requireActivity()).get(PacketModeViewModel.class);

        binding = FragmentTransmitBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.txtViewTransmit;
        packetModeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.sendButtonTransmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    //textView.setText("click");
                    byte[] command = {'<', 0x22, 3}; // Replace with your actual command
                    byte[] response = sendCommandAndGetResponse(command, 3, 1, 1000);
                    if (response != null) {
                        Log.i("Command Response", Arrays.toString(response));
                        // Run the UI update on the main thread
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                textView.setText("sent");
                            });
                        }
                    }

                }).start();
            }
        });

        return root;

    }

    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_USB_DATA_RECEIVED.equals(intent.getAction())) {
                String dataString = intent.getStringExtra("data");
                if (dataString != null) {
                    Log.i("ser string", dataString);
                }
            }
            else if (Constants.ACTION_USB_DATA_BYTES_RECEIVED.equals(intent.getAction())) {
                byte [] bytes = intent.getByteArrayExtra("bytes");
                if (bytes != null) {
                    // Optionally, you can log the byte array to see its contents
                    Log.i("service bytes", Arrays.toString(bytes));
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
    }

    @Override
    public void onPause() {
        // Unregister BroadcastReceiver here
        super.onPause();
        requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
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
}

