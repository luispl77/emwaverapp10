package com.example.emwaver10.ui.scripts;

import androidx.lifecycle.ViewModelProvider;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.emwaver10.jsobjects.CC1101;
import com.example.emwaver10.CommandSender;
import com.example.emwaver10.Constants;
import com.example.emwaver10.jsobjects.Serial;
import com.example.emwaver10.jsobjects.Console;
import com.example.emwaver10.databinding.FragmentScriptsBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ScriptsFragment extends Fragment implements CommandSender {

    private ScriptsViewModel scriptsViewModel;
    private CC1101 cc1101;

    private Serial serial;

    private Console console;
    private FragmentScriptsBinding binding; // Binding class for the fragment_scripts.xml layout

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Initialize view binding
        binding = FragmentScriptsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scriptsViewModel = new ViewModelProvider(this).get(ScriptsViewModel.class);

        cc1101 = new CC1101(this);

        serial = new Serial(getContext(), this);

        console = new Console(getContext());

        initializeScripts();


        // Populate spinner with filenames
        String[] fileNames = getJavaScriptFileNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFiles.setAdapter(adapter);
        // Listener for spinner selection
        binding.spinnerFiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadFileContent(fileNames[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Button listeners for save and rename
        binding.buttonSave.setOnClickListener(v -> saveFile());
        binding.buttonRename.setOnClickListener(v -> renameFile());


        // Set up your button click listeners
        binding.executeScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    String jsCode = binding.jsCodeInput.getText().toString(); // Get code from EditText
                    ScriptsEngine scriptsEngine = new ScriptsEngine(cc1101, scriptsViewModel, serial, console);
                    scriptsEngine.executeJavaScript(jsCode);
                }).start();
            }
        });


        return root;
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
    public void onStop() {
        super.onStop();
        //requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
        //Log.i("onStop", "receiver unregistered");
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers

        IntentFilter filterBytes = new IntentFilter(Constants.ACTION_USB_DATA_BYTES_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filterBytes);
        //Log.i("onStart", "receiver registered");
    }

    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
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
                        scriptsViewModel.addResponseByte(b);
                    }
                }
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for avoiding memory leaks
    }

    @Override
    public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
        // Send the command
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_BYTES_TO_SERVICE);
        intent.putExtra("bytes", command);
        getContext().sendBroadcast(intent);

        long startTime = System.currentTimeMillis(); // Start time for timeout

        // Wait for the response with timeout
        while (scriptsViewModel.getResponseQueueSize() < expectedResponseSize) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                //broadcastTerminalString("Timeout occured");
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
        return scriptsViewModel.getAndClearResponse(expectedResponseSize);
    }

    private void loadFileContent(String fileName) {
        File file = new File(getContext().getFilesDir(), fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                fis.close();
                String content = new String(buffer, "UTF-8");
                binding.jsCodeInput.setText(content);
            } catch (IOException ex) {
                ex.printStackTrace();
                showToastOnUiThread("Error loading file");
            }
        }
    }

    private void saveFile() {
        String selectedFile = binding.spinnerFiles.getSelectedItem().toString();
        String fileContent = binding.jsCodeInput.getText().toString();

        try {
            FileOutputStream fos = getContext().openFileOutput(selectedFile, getContext().MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.close();
            showToastOnUiThread("File saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
            showToastOnUiThread("Error saving file");
        }
    }

    private void renameFile() {
        String oldFileName = binding.spinnerFiles.getSelectedItem().toString();
        String newFileName = binding.editTextNewFileName.getText().toString();

        File oldFile = new File(getContext().getFilesDir(), oldFileName);
        File newFile = new File(getContext().getFilesDir(), newFileName);

        if (oldFile.renameTo(newFile)) {
            showToastOnUiThread("File renamed successfully");
            // Update spinner and any other relevant UI components
            // Refresh file names in the spinner
        } else {
            showToastOnUiThread("Error renaming file");
        }
    }


    private String[] getJavaScriptFileNames() {
        File dir = getContext().getFilesDir();
        FilenameFilter filter = (dir1, name) -> name.endsWith(".js");
        File[] files = dir.listFiles(filter);

        if (files != null) {
            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                fileNames[i] = files[i].getName();
            }
            return fileNames;
        } else {
            return new String[0]; // Return an empty array if no files are found
        }
    }



    private void initializeScripts() {
        String[] assetFileNames = new String[]{"script1.js", "script2.js", "script3.js"};
        for (String fileName : assetFileNames) {
            File file = new File(getContext().getFilesDir(), fileName);
            if (!file.exists()) {
                copyFileFromAssets(fileName);
            }
        }
    }


    private void copyFileFromAssets(String fileName) {
        try {
            InputStream is = getContext().getAssets().open(fileName);
            FileOutputStream fos = new FileOutputStream(new File(getContext().getFilesDir(), fileName));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
}
