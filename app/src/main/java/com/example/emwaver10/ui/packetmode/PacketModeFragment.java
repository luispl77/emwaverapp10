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

import java.util.Arrays;

public class PacketModeFragment extends Fragment {

    private FragmentPacketModeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        PacketModeViewModel packetModeViewModel =
                new ViewModelProvider(this).get(PacketModeViewModel.class);

        binding = FragmentPacketModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textPacketMode;
        packetModeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendReadCommandToCC1101();
                byte [] readBurst = {'<', 0x22, 3}; // read burst command
                //byte [] readBurst = {'i', 'n', 'i', 't'}; // read burst command
                Log.i("Byte Array btn", Arrays.toString(readBurst));
                sendByteDataToService(readBurst);
                //String command = "test01";
                //sendDataToService(command);
            }
        });

        // Initialize and populate the table
        initializeAndPopulateTable();

        return root;
    }

    private void sendReadCommandToCC1101() {
        byte addr = 0x20; // Starting address
        byte len = 10;    // Number of bytes to read

        String command = "<" + String.format("%02X", addr) + String.format("%02X", len);
        sendDataToService(command);
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
        requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data
        super.onStop();
    }
}
