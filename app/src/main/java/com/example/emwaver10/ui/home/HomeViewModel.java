package com.example.emwaver10.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel  extends ViewModel  {
    private final MutableLiveData<String> terminalData = new MutableLiveData<>();

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
}