package com.example.emwaver10;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

public class SerialService extends Service implements SerialInputOutputManager.Listener {

    private SerialInputOutputManager ioManager;
    private MutableLiveData<String> liveData; // Example LiveData for communication
    private UsbSerialPort finalPort = null;

    // Register the BroadcastReceiver
    private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (Constants.ACTION_CONNECT_USB.equals(intent.getAction())) {
            // Connect serial device
            try {
                onConnectClick();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Constants.ACTION_SEND_DATA_TO_SERVICE.equals(intent.getAction())) {
            String userInput = intent.getStringExtra("userInput");
            // Send the received data over USB.
            Log.i("ser", "service received data: " + userInput);
            assert userInput != null;
            byte[] byteArray = userInput.getBytes();
            try {
                finalPort.write(byteArray, 2000);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        liveData = new MutableLiveData<>();
        // Register receivers for listening for broadcasts from Terminal fragment.
        IntentFilter filterConnectButton = new IntentFilter(Constants.ACTION_CONNECT_USB);
        registerReceiver(connectReceiver, filterConnectButton); // Receiver for the connect button in terminal.
        IntentFilter filterData = new IntentFilter(Constants.ACTION_SEND_DATA_TO_SERVICE);
        registerReceiver(connectReceiver, filterData); // Receiver for the data inputted in terminal fragment and entered, to then be sent over USB.
        // todo: fix the security warning about visibility of the broadcast receiver
    }

    // Callback that runs when the service is started. Not useful for now.
    // START_STICKY: If the service is killed by the system, recreate it, but do not redeliver the last intent. Instead, the system calls onStartCommand with a null intent, unless there are pending intents to start the service. This is suitable for services that are continually running in the background (like music playback) and that don't rely on the intent data.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Called when new data arrives on the USB port that is connected. Sends the data over to the TerminalViewModel to update UI and show the communication.
    @Override
    public void onNewData(byte[] data) {
        // Update LiveData or send Broadcast
        String dataString = new String(data);
        liveData.postValue(dataString);
        // Or send a broadcast
        Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);
        intent.putExtra("data", dataString);
        sendBroadcast(intent);
    }

    @Override
    public void onRunError(Exception e) {

    }

    //Finds the port in which the USB device is connected to. Connects to the driver and returns the port.
    public UsbSerialPort connectUSBAndReturnPort () throws IOException {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);

        //USB PERMISSION CODE:
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.emwaver10.GRANT_USB"), flags);
        manager.requestPermission(driver.getDevice(), usbPermissionIntent);

        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Toast.makeText(this, "Connection returned null", Toast.LENGTH_SHORT).show();
            return null;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        port.open(connection);
        port.setParameters(Constants.USB_BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        ioManager = new SerialInputOutputManager(port, this);
        ioManager.start();

        return port;
    }

    //Called from the broadcast done in Terminal, when user clicks the connect button.
    public void onConnectClick() throws IOException {
        try {
            finalPort = connectUSBAndReturnPort();
            if (finalPort != null) {
                Toast.makeText(this, "Driver: " + finalPort + " max pkt size: " + finalPort.getReadEndpoint().getMaxPacketSize(), Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(this, "No port available", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectReceiver); //will this ever be destroyed? perhaps when app closes.
    }

}
