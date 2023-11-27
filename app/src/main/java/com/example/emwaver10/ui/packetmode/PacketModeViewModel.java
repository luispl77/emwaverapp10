package com.example.emwaver10.ui.packetmode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketModeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private MutableLiveData<Queue<Byte>> responseQueueLiveData;
    private Queue<Byte> responseQueue = new ConcurrentLinkedQueue<>();

    public PacketModeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("receive mode");

        responseQueueLiveData = new MutableLiveData<>();
        responseQueueLiveData.setValue(new LinkedList<>());
    }

    public LiveData<String> getText() {
        return mText;
    }

    //queue for storing the latest responses
    public LiveData<Queue<Byte>> getResponseQueue() {
        return responseQueueLiveData;
    }

    public void addResponseByte(Byte responseByte) {
        Queue<Byte> currentQueue = responseQueueLiveData.getValue();
        if (currentQueue != null) {
            currentQueue.add(responseByte);
            responseQueueLiveData.setValue(currentQueue); // Trigger LiveData update
            responseQueue.add(responseByte);
        }
    }
    // Method to retrieve and clear data from the queue
    public byte[] getAndClearResponse(int expectedSize) {
        byte[] response = new byte[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            response[i] = responseQueue.poll(); // or handle nulls if necessary
        }
        return response;
    }

    public int getResponseQueueSize() {
        return responseQueue.size();
    }

    public void clearResponseQueue() {
        Queue<Byte> currentQueue = responseQueueLiveData.getValue();
        if (currentQueue != null) {
            currentQueue.clear();
            responseQueueLiveData.setValue(currentQueue); // Trigger LiveData update
        }
    }
}
