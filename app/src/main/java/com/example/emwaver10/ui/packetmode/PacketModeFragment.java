package com.example.emwaver10.ui.packetmode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.emwaver10.Constants;
import com.example.emwaver10.databinding.FragmentPacketModeBinding;
import com.example.emwaver10.ui.terminal.TerminalViewModel;

import java.util.Arrays;
import java.util.Queue;

public class PacketModeFragment extends Fragment {

    private FragmentPacketModeBinding binding;

    private PacketModeViewModel packetModeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(this).get(PacketModeViewModel.class);

        binding = FragmentPacketModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textPacketMode;
        packetModeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte [] readBurst = {'<', 0x22, 3}; // read burst command
                Log.i("Byte Array btn", Arrays.toString(readBurst));
                sendByteDataToService(readBurst);
            }
        });

        // Initialize and populate the table
        initializeAndPopulateTable();

        return root;
    }

    private void sendCommandAndWaitForResponse(byte[] command) {
        final int[] sizeBefore = new int[1];
        packetModeViewModel.getResponseQueue().observe(getViewLifecycleOwner(), queue -> {
            if (queue.size() > sizeBefore[0]) {
                // Response has been received
                //Byte response = queue.poll(); // or process the whole queue as needed
                Log.i("Queue Contents", "queue size: " + queue.size() + "queue size before: " + sizeBefore[0]);
                logQueueContents(queue);
                // Handle the response
            }
        });

        // Send command
        sizeBefore[0] = packetModeViewModel.getResponseQueue().getValue().size();
        sendByteDataToService(command);
    }

    private void logQueueContents(Queue<Byte> queue) {
        StringBuilder sb = new StringBuilder();
        for (Byte b : queue) {
            sb.append(String.format("%02X ", b)); // Formatting each byte as Hex
        }
        Log.i("Queue Contents", sb.toString());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers
    }

    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_USB_DATA_RECEIVED.equals(intent.getAction())) {
                String dataString = intent.getStringExtra("data");
                if (dataString != null) {
                    Log.i("service string", dataString);
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


    private void sendDataToService(String userInput) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_TO_SERVICE);
        intent.putExtra("userInput", userInput);
        requireActivity().sendBroadcast(intent);
    }

    private void sendByteDataToService(byte[] bytes) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_BYTES_TO_SERVICE);
        intent.putExtra("bytes", bytes);
        requireActivity().sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
        super.onStop();
    }
}
