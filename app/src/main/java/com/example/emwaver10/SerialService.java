package com.example.emwaver10;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SerialService extends Service implements SerialInputOutputManager.Listener {

    static {
        System.loadLibrary("native-lib");
    }

    private SerialInputOutputManager ioManager;

    private UsbSerialPort finalPort = null;

    private UsbDeviceConnection finalConnection = null;

    private ConcurrentLinkedQueue<Byte> responseQueue = new ConcurrentLinkedQueue<>();

    private final IBinder binder = new LocalBinder();

    private native void addToBuffer(byte[] data);

    public native int getBufferLength();

    public native byte[] pollData(int length);

    public native void clearBuffer();

    public native Object[] compressData(int rangeStart, int rangeEnd, int numberBins);

    public native void setRecording(boolean recording);

    public native boolean getRecording();

    public native boolean getRecordingContinuous();

    public native void setRecordingContinuous(boolean recording);



    public class LocalBinder extends Binder {
        public SerialService getService() {
            // Return this instance of SerialService so clients can call public methods
            return SerialService.this;
        }
    }

    public void addResponseByte(Byte responseByte) {
        responseQueue.add(responseByte);
    }
    // Method to retrieve and clear data from the queue
    public byte[] getAndClearResponse(int expectedSize) {
        byte[] response = new byte[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            response[i] = responseQueue.poll(); // or handle nulls if necessary
        }
        return response;
    }
    public ConcurrentLinkedQueue<Byte> getResponseQueue() {
        return responseQueue;
    }
    // Method to clear the queue, if needed
    public void clearResponseQueue() {
        responseQueue.clear();
    }

    public void write(byte[] bytes){
        if(bytes != null && finalPort != null) {
            try {
                finalPort.write(bytes, 2000);
            } catch (IOException e) {
                Log.e("SerialService", "Error writing to port: ", e);
            }
        }
        else{
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
        }
    }


    // Register the BroadcastReceiver
    private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_CONNECT_USB.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Permission granted, initiate connection
                            try {
                                finalPort = connectUSBSerialDevice(device);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(context, "USB Serial Permission Granted", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "USB Serial Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }else if (Constants.ACTION_INITIATE_USB_CONNECTION.equals(intent.getAction())) {
                connectUSBSerial(); // Method to start USB connection process

            }else if (Constants.ACTION_SEND_DATA_TO_SERVICE.equals(intent.getAction())) {
                String userInput = intent.getStringExtra("userInput");
                // Send the received data over USB.
                Log.i("service", userInput);
                assert userInput != null;
                byte[] byteArray = userInput.getBytes();
                try {
                    if(byteArray != null && finalPort != null)
                        finalPort.write(byteArray, 2000);
                    else{
                        Toast.makeText(context, "No devices found", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.e("SerialService", "Error writing to port: ", e);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Register receivers for listening for broadcasts from Serial fragment.
        IntentFilter filterConnectButton = new IntentFilter(Constants.ACTION_CONNECT_USB);
        registerReceiver(connectReceiver, filterConnectButton); // Receiver for the connect button in terminal.
        IntentFilter filterData = new IntentFilter(Constants.ACTION_SEND_DATA_TO_SERVICE);
        registerReceiver(connectReceiver, filterData); // Receiver for the data inputted in terminal fragment and entered, to then be sent over USB.
        IntentFilter filterBytes = new IntentFilter(Constants.ACTION_SEND_DATA_BYTES_TO_SERVICE);
        registerReceiver(connectReceiver, filterBytes); // Receiver for the data inputted in terminal fragment and entered, to then be sent over USB.
        // todo: fix the security warning about visibility of the broadcast receiver

        IntentFilter filter = new IntentFilter(Constants.ACTION_CONNECT_USB_BOOTLOADER);
        registerReceiver(connectReceiver, filter);

        IntentFilter filterConnection = new IntentFilter(Constants.ACTION_INITIATE_USB_CONNECTION);
        registerReceiver(connectReceiver, filterConnection);
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
        return binder;
    }

    //Called when new data arrives on the USB port that is connected. Sends the data over to the TerminalViewModel to update UI and show the communication.
    @Override
    public void onNewData(byte[] data) {
        //cpp environment storing of data
        addToBuffer(data);

        //terminal intents. the terminal does not operate when in continuous mode.
        /*if(!getRecordingContinuous()){
            //for terminal
           sendIntentToTerminal(data);
        }*/
    }

    public void sendIntentToTerminal(byte[] data) {
        Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);
        intent.putExtra("data", data);
        sendBroadcast(intent);
    }

    @Override
    public void onRunError(Exception e) {

    }

    //Finds the port in which the USB device is connected to. Connects to the driver and returns the port.
    public void connectUSBSerial() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        // Check if permission is already granted
        if (!manager.hasPermission(device)) {
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    new Intent(Constants.ACTION_CONNECT_USB)
                            .putExtra(UsbManager.EXTRA_DEVICE, device),
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
            );
            manager.requestPermission(device, usbPermissionIntent);
        } else {
            // Permission is already granted, open the device here or handle as needed
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
            try {
                finalPort = connectUSBSerialDevice(device);
                Toast.makeText(this, "Connected!\nDriver: " + finalPort + "\n max pkt size: " + finalPort.getReadEndpoint().getMaxPacketSize(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // You might want to initiate connection here if permission is already granted
        }
    }

    private UsbSerialPort connectUSBSerialDevice(UsbDevice device) throws IOException {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) {
            Toast.makeText(this, "Connection returned null", Toast.LENGTH_SHORT).show();
            return null;
        }

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        UsbSerialPort port = driver.getPorts().get(0); // Assuming there's only one port
        port.open(connection);
        port.setParameters(Constants.USB_BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        ioManager = new SerialInputOutputManager(port, this);
        ioManager.start();

        return port;
    }


    public void connectUSBFlash() {
        final int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
        final int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        boolean deviceFound = false;
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == USB_VENDOR_ID && device.getProductId() == USB_PRODUCT_ID) {
                deviceFound = true;
                // Check if permission is already granted
                if (!manager.hasPermission(device)) {
                    PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(
                            this,
                            0,
                            new Intent(Constants.ACTION_CONNECT_USB_BOOTLOADER)
                                    .putExtra(UsbManager.EXTRA_DEVICE, device),
                            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
                    );
                    manager.requestPermission(device, usbPermissionIntent);
                } else {
                    // Permission is already granted, handle as needed
                    Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

        if (!deviceFound) {
            Toast.makeText(this, "No STM32 bootloader connected", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectReceiver); //will this ever be destroyed? perhaps when app closes.
    }

}
