package com.example.emwaver10.ui.terminal;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.emwaver10.Constants;
import com.example.emwaver10.SerialService;
import com.example.emwaver10.databinding.FragmentTerminalBinding;


public class TerminalFragment extends Fragment{
    private FragmentTerminalBinding binding;
    private EditText terminalTextInput;
    private TextView terminalText;
    private TerminalViewModel terminalViewModel;
    private boolean filterEnabled = true;

    // In your onCreateView or onCreate method:



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot(); // inflate fragment_terminal.xml

        terminalText = binding.terminalText; //get bindings
        terminalTextInput = binding.terminalTextInput;
        binding.terminalText.setMovementMethod(new ScrollingMovementMethod()); // Set the TextView as scrollable

        // Observe the LiveData and update the UI accordingly
        terminalViewModel = new ViewModelProvider(this).get(TerminalViewModel.class);
        terminalViewModel.getTerminalData().observe(getViewLifecycleOwner(), text -> {
            terminalText.setText(text);
        });

        // Display input from EditText to TextView when the user hits 'Enter'
        terminalTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = terminalTextInput.getText().toString();
                sendUserInputToService(userInput); // Send to SerialService for transmitting over USB
                terminalViewModel.appendData(userInput);
                terminalTextInput.setText("");
            }
            return false;
        });

        binding.filterCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterEnabled = isChecked;
        });

        return root;
    }



    @Override
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers
    }

    // Broadcast receiver for data coming from SerialService background USB service. Updates terminal live UI.
    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_USB_DATA_RECEIVED.equals(intent.getAction())) {
                byte [] data = intent.getByteArrayExtra("data");
                //Log.i("terminal", dataString);
                StringBuilder stringBuilder = new StringBuilder();

                for (byte b : data) {
                    if (filterEnabled || b == 0x0A || b == 0x09 || (b >= 32 && b <= 126)) {
                        // Append the byte as a character if filter is disabled or it's a printable character, newline, or tab
                        stringBuilder.append((char) b);
                    } else {
                        // Convert non-printable characters to hex string
                        stringBuilder.append(String.format("[0x%02X]", b));
                    }
                }

                String dataString = stringBuilder.toString();
                terminalViewModel.appendData(dataString); // Update UI by appending the USB data received in TerminalViewModel.
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //connect button
        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Constants.ACTION_INITIATE_USB_CONNECTION);
                getContext().sendBroadcast(intent); // Send intent to SerialService to initiate USB connection
            }
        });
        //clear-terminal-text button
        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                terminalViewModel.setData("");
                terminalText.setText("");
            }
        });
    }

    @Override
    public void onStop() {
        //requireActivity().unregisterReceiver(usbDataReceiver); //don't call this to leave the broadcast of the USB data received active.
        super.onStop();
    }

    //Broadcasts any data over to the SerialService. SerialService then transmits the data over USB.
    private void sendUserInputToService(String userInput) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_TO_SERVICE);
        intent.putExtra("userInput", userInput);
        requireActivity().sendBroadcast(intent);
    }
}
