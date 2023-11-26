package com.example.emwaver10.ui.packetmode;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.emwaver10.Constants;
import com.example.emwaver10.databinding.FragmentReceiveBinding;

import java.util.Arrays;

public class ReceiveFragment extends Fragment {

    private FragmentReceiveBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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

    private void sendByteDataToService(byte[] bytes) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_BYTES_TO_SERVICE);
        intent.putExtra("bytes", bytes);
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
}