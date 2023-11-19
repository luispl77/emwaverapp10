package com.example.emwaver10;

public class Constants {
    // Action string for the broadcast when connecting USB
    public static final String ACTION_CONNECT_USB = "com.example.ACTION_CONNECT_USB";

    // Action string for the broadcast when data is received from USB
    public static final String ACTION_USB_DATA = "com.example.ACTION_USB_DATA";

    // Extra keys for intents or bundles (if needed)
    public static final String EXTRA_USB_DATA = "com.example.EXTRA_USB_DATA";

    // Any other constant values that are used across multiple classes
    public static final int USB_BAUD_RATE = 115200;

    // Private constructor to prevent instantiation
    private Constants() {
        // This utility class is not publicly instantiable
    }
}

