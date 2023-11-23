package com.example.emwaver10;

public class Constants {
    // Action string for the broadcast when connecting USB
    public static final String ACTION_CONNECT_USB = "com.example.ACTION_CONNECT_USB";

    // Action string for the broadcast when sending data (from terminal) to be transmitted over USB in SerialService
    public static final String ACTION_SEND_DATA_TO_SERVICE = "com.example.ACTION_SEND_DATA_TO_SERVICE";

    public static final String ACTION_SEND_DATA_BYTES_TO_SERVICE = "com.example.ACTION_SEND_DATA_BYTES_TO_SERVICE";

    // Action string for the broadcast when data is received from USB
    public static final String ACTION_USB_DATA_RECEIVED = "com.example.ACTION_USB_DATA";

    // Any other constant values that are used across multiple classes
    public static final int USB_BAUD_RATE = 115200;

    // Private constructor to prevent instantiation
    private Constants() {
        // This utility class is not publicly instantiable
    }
}

