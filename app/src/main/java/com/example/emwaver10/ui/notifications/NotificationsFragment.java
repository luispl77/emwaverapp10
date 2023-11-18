package com.example.emwaver10.ui.notifications;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.emwaver10.Dfu;
import com.example.emwaver10.Usb;
import com.example.emwaver10.databinding.FragmentNotificationsBinding;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel notificationsViewModel;

    private Usb usb;
    private Dfu dfu;
    private TextView status;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);

        // Observe the LiveData from the ViewModel
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), newText -> {
            binding.textNotifications.setText(newText);
        });

        dfu = new Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID, this);
        dfu.setListener((Dfu.DfuListener) this);



        // Locate the connect button
        Button writeVerify = binding.writeAll;
        // Set click listener to the fragment
        writeVerify.setOnClickListener((View.OnClickListener) this);
        writeVerify.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int startAddress = 0x08000000;
                        int endAddress = 0x08000080;
                        byte[] block = new byte[4];
                        byte[] blockReading = new byte[4];
                        int addressOffset = 0; // Offset from the start address
                        int currentAddress = 0;

                        try {
                            // Open the dfu.dfu file from assets
                            Activity activity = getActivity();
                            if (activity != null) {
                                InputStream inputStream = activity.getAssets().open("dfu.dfu");
                                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                                while (currentAddress < endAddress && bufferedInputStream.read(block) != -1) {
                                    currentAddress = startAddress + addressOffset;
                                    try {
                                        // Write the block to the dfu at the current address
                                        dfu.writeBlock(currentAddress, block, 0); // blockNumber is always 0
                                        dfu.readBlock(currentAddress, blockReading, 0);
                                        if(!Arrays.equals(block, blockReading))
                                            continue; //if verification fails, go back and repeat
                                        final int finalCurrentAddress = currentAddress; // final copy

                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                status.append("Data written to address 0x" + Integer.toHexString(finalCurrentAddress) + "\n");
                                            }
                                        });
                                        addressOffset += block.length;

                                    } catch (Exception e) {
                                        final String errorMessage = "Error writing to dfu at address 0x" + Integer.toHexString(currentAddress) + ": " + e.getMessage() + "\n";
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                status.append(errorMessage);
                                            }
                                        });
                                    }
                                }

                                bufferedInputStream.close();
                                inputStream.close();
                            }

                        } catch (IOException e) {
                            final String ioErrorMessage = "Error opening dfu.dfu file: " + e.getMessage() + "\n";
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        status.append(ioErrorMessage);
                                    }
                                });
                            }
                        }
                    }
                }).start();

            }
        });


        return binding.getRoot();
    }

    // Method to append text to the ViewModel
    public void appendTextToViewModel(String text) {
        notificationsViewModel.appendText(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
