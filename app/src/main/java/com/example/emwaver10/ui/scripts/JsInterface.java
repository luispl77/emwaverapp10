package com.example.emwaver10.ui.scripts;

import com.example.emwaver10.CommandSender;

public interface JsInterface extends CommandSender {
    void broadcastTerminalString(String message);

    void sendCommandString(String userInput, int delayMillis);
    void sendCommand(byte [] bytes, int delayMillis);
}

