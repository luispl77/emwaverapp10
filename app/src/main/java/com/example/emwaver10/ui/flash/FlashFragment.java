package com.example.emwaver10.ui.flash;

import static android.app.Activity.RESULT_OK;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater; //
import android.view.View;
import android.view.ViewGroup; //
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emwaver10.databinding.FragmentFlashBinding;

import java.util.Arrays;

public class FlashFragment extends Fragment implements Handler.Callback, Usb.OnUsbChangeListener, Dfu.DfuListener {
    private FragmentFlashBinding binding;
    private FlashViewModel notificationsViewModel;

    private Usb usb;
    private Dfu dfu;
    private static final int REQUEST_CODE_ATTACH = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100; // A unique request code
    private TextView status;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationsViewModel = new ViewModelProvider(this).get(FlashViewModel.class);
        binding = FragmentFlashBinding.inflate(inflater, container, false);

        // Check for the permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        dfu = new Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID, this);
        dfu.setListener(this);

        status = binding.status;

        Button clearTxtView = binding.clearTxt;
        clearTxtView.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("");
            }
        });

        Button writeBlockButton = binding.writeBlockButton;
        writeBlockButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                int BLOCK_SIZE = 2048;
                int startAddress = 0x08000000;
                byte [] buffer = new byte[BLOCK_SIZE];
                // Initialize all elements to 0x69
                byte valueToSet = (byte) 0x69; // Casting is important as 0x69 is an int literal
                for (int i = 0; i < BLOCK_SIZE; i++)
                    buffer[i] = valueToSet;
                try {

                    dfu.massErase();
                    dfu.setAddressPointer(0x08000000);

                    byte[] block = new byte[BLOCK_SIZE];
                    Arrays.fill(block, (byte) 0x69); // Fill the block with 0x69
                    dfu.writeBlock(block, 2, BLOCK_SIZE); // Assuming writeBlock method is implemented in your Dfu class
                    //status.append("wrote flash\n");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Button massEraseButton = binding.massEraseButton;
        massEraseButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dfu.massErase();
                } catch (Exception e) {
                    status.append(e.toString());
                }
            }
        });

        Button readFlashButton = binding.readFlashButton;
        readFlashButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dfu.readFlash(0x6BE0);
                } catch (Exception e) {
                    status.append(e.toString());
                }
            }
        });

        Button writeFlashButton = binding.writeFlashButton;
        writeFlashButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dfu.massErase();
                    dfu.setAddressPointer(0x08000000);

                    dfu.writeFlash();
                } catch (Exception e) {
                    status.append(e.toString());
                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Setup USB
        usb = new Usb(requireContext());
        usb.setUsbManager((UsbManager) requireContext().getSystemService(Context.USB_SERVICE));
        usb.setOnUsbChangeListener(this);

        // Handle two types of intents. Device attachment and permission
        requireContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(Usb.ACTION_USB_PERMISSION));
        requireContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        requireContext().registerReceiver(usb.getmUsbReceiver(), new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        // Handle case where USB device is connected before app launches;
        // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
        if (usb.isConnected()){
            usb.requestPermission(requireContext(), Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // USB
        dfu.setUsb(null);
        usb.release();
        try {
            requireContext().unregisterReceiver(usb.getmUsbReceiver());
        } catch (IllegalArgumentException e) {
        // Already unregistered
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Set type for file (e.g., "image/*" for images)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            someActivityResultLauncher.launch(Intent.createChooser(intent, "Select a file"));
        } catch (android.content.ActivityNotFoundException ex) {
            // Handle if no file chooser is available
            Toast.makeText(requireContext(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    public ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    if (data != null) {
                        Uri fileUri = data.getData();
                        // Handle the selected file
                        // ...
                    }
                }
            });

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        return false;
    }

    @Override
    public void onStatusMsg(String msg) {
        // TODO since we are appending we should make the TextView scrollable like a log
        status.append(msg);
    }

    @Override
    public void onUsbConnected() {
        final String deviceInfo = usb.getDeviceInfo(usb.getUsbDevice());

        status.setText(deviceInfo);
        dfu.setUsb(usb);
    }
}
