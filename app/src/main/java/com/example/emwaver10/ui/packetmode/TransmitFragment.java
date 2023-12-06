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
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emwaver10.Constants;
import com.example.emwaver10.R;
import com.example.emwaver10.databinding.FragmentReceiveBinding;
import com.example.emwaver10.databinding.FragmentTransmitBinding;

import java.util.Arrays;
import java.util.List;

public class TransmitFragment extends Fragment {

    private FragmentTransmitBinding binding;

    private PacketModeViewModel packetModeViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        packetModeViewModel = new ViewModelProvider(requireActivity()).get(PacketModeViewModel.class);

        binding = FragmentTransmitBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        binding.sendButtonTransmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    byte [] teslaSignal = {50, -52, -52, -53, 77, 45, 74, -45, 76, -85, 75, 21, -106, 101, -103, -103, -106, -102, 90, -107, -90, -103, 86, -106, 43, 44, -53, 51, 51, 45, 52, -75, 43, 77, 50, -83, 40};
                    //sendData(teslaSignal, teslaSignal.length, 300);
                    writeReg((byte)0x10, (byte)0x69);
                    byte reading = readReg((byte)0x10);
                    Log.i("reading", "" + reading);
                }).start();
            }
        });

        binding.setDataRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    if(setDataRate(2500)){

                        showToastOnUiThread("Data rate set successfully");
                    }
                    else{

                        showToastOnUiThread("Error setting data rate");

                    }
                }).start();
            }
        });
        binding.initTransmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    sendInit();
                }).start();
            }
        });

        // Set an OnClickListener for the button
        binding.sendPayloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String payload = binding.payloadTextInputEditText.getText().toString();
                Log.i("Payload", payload);
                byte [] payload_bytes = convertHexStringToByteArray(payload);

                new Thread(() -> {
                    sendData(payload_bytes, payload_bytes.length, 300);
                }).start();
            }
        });

        String[] modulations = getResources().getStringArray(R.array.modulations);
        ArrayAdapter arrayAdapter = new ArrayAdapter(requireContext(), R.layout.dropdown_item, modulations);
        binding.autoCompleteTextView.setAdapter(arrayAdapter);

        return root;

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


    private void spiStrobe(byte commandStrobe){
        byte [] command = new byte[2];
        byte [] response = new byte[1];
        command[0] = '%'; //command strobe character
        command[1] = commandStrobe;
        response = sendCommandAndGetResponse(command, 1, 1, 1000);
        Log.i("spiStrobe", Arrays.toString(response));
    }

    private void writeBurstReg(byte addr, byte[] data, byte len){
        byte [] command = new byte[data.length+3];
        byte [] response = new byte[1];
        command[0] = '>'; //write burst reg character
        command[1] = addr; //burst write >[addr][len][data]
        command[2] = len;
        System.arraycopy(data, 0, command, 3, data.length); // Efficient array copy
        response = sendCommandAndGetResponse(command, 1, 1, 1000);
        Log.i("writeBurstReg", Arrays.toString(response));
    }

    private byte [] readBurstReg(byte addr, int len){
        byte [] command = new byte[3];
        byte [] response = new byte[len];
        command[0] = '<'; //read burst reg character
        command[1] = addr; ////burst read <[addr][len]
        command[2] = (byte)len;
        response = sendCommandAndGetResponse(command, (byte)len, 1, 1000);
        Log.i("readBurstReg", Arrays.toString(response));
        return response;
    }

    private byte readReg(byte addr){
        byte [] command = new byte[2];
        byte [] response = new byte[1];
        command[0] = '?'; //read reg character
        command[1] = addr; //single read ?[addr]
        response = sendCommandAndGetResponse(command, (byte)1, 1, 1000);
        Log.i("readReg", Arrays.toString(response));
        return response[0];
    }

    private void writeReg(byte addr, byte data){
        byte [] command = new byte[3];
        byte [] response = new byte[1];
        command[0] = '!'; //write reg character
        command[1] = addr; //single write ![addr][data]
        command[2] = data;
        response = sendCommandAndGetResponse(command, 1, 1, 1000);
        Log.i("writeReg", Arrays.toString(response));
    }


    private void sendData(byte [] txBuffer, int size, int t) {
        byte CC1101_TXFIFO = 0x3F;
        byte CC1101_SIDLE = 0x36;
        byte CC1101_STX = 0x35;
        byte CC1101_SFTX = 0x3B;
        writeBurstReg(CC1101_TXFIFO, txBuffer, (byte) size);     //write data to send
        spiStrobe(CC1101_SIDLE);
        spiStrobe(CC1101_STX);                          //start send
        try {
            Thread.sleep(t);                                //wait for transmission to be done
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        spiStrobe(CC1101_SFTX);                         //flush TXfifo
    }

    public void sendInit(){
            byte[] command = {'t', 'x', 'i', 'n', 'i', 't'}; // Replace with your actual command
            String responseString = "Transmit init done\n";
            int length = responseString.length();
            byte[] response = sendCommandAndGetResponse(command, length, 1, 1000);
            if (response != null) {
                Log.i("Command Response", Arrays.toString(response));
            }
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


    public boolean setDataRate(int bitRate) {
        // Constants for the DRATE register calculation
        final double F_OSC = 26_000_000; // Oscillator frequency in Hz
        final int DRATE_M_MAX = 255; // 8-bit DRATE_M has max value 255
        final int DRATE_E_MAX = 15;  // 4-bit DRATE_E has max value 15
        double target = bitRate * Math.pow(2, 28) / F_OSC;
        double minDifference = Double.MAX_VALUE;
        int bestM = 0;
        int bestE = 0;

        // Find the closest DRATE_M and DRATE_E for the desired bit rate
        for (int e = 0; e <= DRATE_E_MAX; e++) {
            for (int m = 0; m <= DRATE_M_MAX; m++) {
                double currentValue = (256 + m) * Math.pow(2, e);
                double difference = Math.abs(currentValue - target);
                if (difference < minDifference) {
                    minDifference = difference;
                    bestM = m;
                    bestE = e;
                }
            }
        }

        // Log the values found
        Log.i("ModemConfig", "DRATE_M: " + bestM + ", DRATE_E: " + bestE);

        // Read the current value of the MDMCFG4 register to keep the first word
        int CC1101_MDMCFG4 = 0x10;
        byte[] readValue = readBurstReg((byte)CC1101_MDMCFG4, 2);
        Log.i("ModemConfig", "CC1101_MDMCFG4: " + readValue[0] + ", CC1101_MDMCFG3: " + readValue[1]);
        int firstWord = readValue[0] & 0xF0; // Ensure it is treated as unsigned

        // Combine the read first word with the calculated DRATE_M
        int combinedE = firstWord | (bestE & 0x0F); // Assumes the first word is the high byte

        // Log the values found
        Log.i("ModemConfig", "DRATE_M: " + bestM + ", DRATE_E: " + combinedE);

        // Write the combined value and DRATE_E to the modem configuration registers
        byte[] mdmcfg = {(byte) combinedE, (byte) bestM};
        writeBurstReg((byte) CC1101_MDMCFG4, mdmcfg, (byte) 2);

        //confirm reading
        readValue = readBurstReg((byte)CC1101_MDMCFG4, 2);
        Log.i("ModemConfig", "CC1101_MDMCFG4: " + (int)readValue[0] + ", CC1101_MDMCFG3: " + (int)readValue[1]);
        if(Arrays.equals(readValue, mdmcfg)){
            return true;
        }
        else{
            return false;
        }
    }

    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }


}

