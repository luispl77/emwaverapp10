package com.example.emwaver10.ui.terminal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TerminalViewModel extends ViewModel  {
    private final MutableLiveData<String> terminalData = new MutableLiveData<>();

    public MutableLiveData<String> getTerminalData() {
        return terminalData;
    }

    public void appendData(String data) {
        String currentData = terminalData.getValue();
        if (currentData == null) {
            currentData = "";
        }
        terminalData.setValue(currentData + data);
    }


    // New method to set data to a specific string
    public void setData(String data) {
        terminalData.setValue(data);
    }

}