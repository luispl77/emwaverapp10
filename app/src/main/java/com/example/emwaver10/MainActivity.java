package com.example.emwaver10;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.emwaver10.ui.terminal.TerminalViewModel;
import com.example.emwaver10.ui.terminal.USBConnectionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.emwaver10.databinding.ActivityMainBinding;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    private ActivityMainBinding binding;

    private SerialInputOutputManager ioManager;

    private UsbSerialPort finalPort = null;

    private TerminalViewModel terminalViewModel = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        terminalViewModel = new ViewModelProvider(this).get(TerminalViewModel.class);

        terminalViewModel.getDataToSend().observe(this, data -> {
            // This code will be executed when the data changes
            Toast.makeText(MainActivity.this, "Received data: " + data, Toast.LENGTH_SHORT).show();
            Log.i("debug", "received data"+ data);
        });


        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_terminal, R.id.navigation_packetmode, R.id.navigation_flash)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    public void onConnectClick(View view) throws IOException {
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
        String userInput = "help";
        byte[] byteArray = userInput.getBytes();
        finalPort.write(byteArray, 2000);
    }

    @Override
    public void onNewData(byte[] data) {
        Toast.makeText(this, "on new data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRunError(Exception e) {

    }

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
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        ioManager = new SerialInputOutputManager(port, this);
        ioManager.start();

        return port;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity", "onResume: MainActivity is active");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Add a log statement to indicate that the MainActivity has stopped
        Log.i("MainActivity", "onStop: MainActivity is no longer visible");
    }


}