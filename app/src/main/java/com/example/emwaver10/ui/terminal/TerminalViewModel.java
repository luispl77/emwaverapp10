package com.example.emwaver10.ui.terminal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TerminalViewModel extends ViewModel  {
    private final MutableLiveData<String> terminalData = new MutableLiveData<>();

    private final MutableLiveData<String> dataToSend = new MutableLiveData<>();

    public MutableLiveData<String> getTerminalData() {
        return terminalData;
    }

    public void appendData(String data) {
        if (terminalData.getValue() != null) {
            terminalData.setValue(terminalData.getValue() + "\n> " + data);
        } else {
            terminalData.setValue("> " + data);
        }
    }
    public LiveData<String> getDataToSend() {
        return dataToSend;
    }

    public void sendDataToMainActivity(String data) {
        dataToSend.setValue(data);
    }
}