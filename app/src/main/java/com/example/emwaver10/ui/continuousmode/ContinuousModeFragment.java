package com.example.emwaver10.ui.continuousmode;

import androidx.lifecycle.ViewModelProvider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.emwaver10.CommandSender;
import com.example.emwaver10.R;
import com.example.emwaver10.SerialService;
import com.example.emwaver10.databinding.FragmentContinuousModeBinding;
import com.example.emwaver10.databinding.FragmentScriptsBinding;
import com.example.emwaver10.jsobjects.CC1101;
import com.example.emwaver10.ui.scripts.ScriptsViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContinuousModeFragment extends Fragment implements CommandSender {

    private ContinuousModeViewModel continuousmodeViewModel;

    private FragmentContinuousModeBinding binding; // Binding class for the fragment_scripts.xml layout

    private SerialService serialService;

    LineChart chart = null;

    private int chartMinX = 0;
    private int chartMaxX = 10000;

    private boolean isServiceBound = false;

    private float currentZoomLevel = 1.0f;

    private int prevRangeStart = 0;
    private int prevRangeEnd = 0;

    private CC1101 cc;


    public ScheduledExecutorService scheduler;

    private final int refreshRate = 100; // Refresh rate in milliseconds





    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
            //updateChart(compressDataAndGetDataSet(0, serialService.getBufferLength(), 1000));
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Log.i("service binding", "onServiceDisconnected");
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Initialize view binding
        binding = FragmentContinuousModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshChart, 0, refreshRate, TimeUnit.MILLISECONDS);
        // Todo: make executor function only execute when its recording

        chart = binding.chart;

        CC1101 cc = new CC1101(this);

        continuousmodeViewModel = new ViewModelProvider(this).get(ContinuousModeViewModel.class);

        binding.connectButton.setOnClickListener(v -> {
            serialService.connectUSBSerial();
            //updateVisibleRange();
        });

        binding.initContinuousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInitRxContinuous();
                    //showToastOnUiThread("check terminal");
                }).start();
            }
        });

        binding.getBufferLengthButton.setOnClickListener(v -> {
            int bufferLength = serialService.getBufferLength();
            Toast.makeText(getContext(), "Buffer Length: " + bufferLength, Toast.LENGTH_SHORT).show();
            //refreshChart();
        });

        binding.clearBufferButton.setOnClickListener(v -> {
            serialService.clearBuffer();
            Toast.makeText(getContext(), "buffer cleared" , Toast.LENGTH_SHORT).show();
            updateChart(compressDataAndGetDataSet(continuousmodeViewModel.getVisibleRangeStart(), continuousmodeViewModel.getVisibleRangeEnd(), 1000));
        });

        binding.startRecordingButton.setOnClickListener(v -> {
            String contCommand = "cont";
            byte[] byteArray = contCommand.getBytes();
            serialService.setRecordingContinuous(true);
            serialService.write(byteArray);

        });

        binding.stopRecordingButton.setOnClickListener(v -> {
            String contCommand = "ssss";
            byte[] byteArray = contCommand.getBytes();
            serialService.write(byteArray);
            new Thread(() -> {
                serialService.emptyReadBuffer(); //this function busy waits
                serialService.setRecordingContinuous(false); //wait before the buffer is empty
            }).start();

            chartMaxX = serialService.getBufferLength();
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
            xAxis.setAxisMaximum(chartMaxX);
            updateChart(compressDataAndGetDataSet(continuousmodeViewModel.getVisibleRangeStart(), continuousmodeViewModel.getVisibleRangeEnd(), 1000));
        });


        binding.showPulseEdgesButton.setOnClickListener(v -> {
            toggleVerticalLinesOnChart(serialService.findPulseEdges(32, 10, 4));
        });

        initChart();



        chart.setOnChartGestureListener(new OnChartGestureListener() {
            //region useless chart listeners
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }
            //endregion
            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                float newZoomLevel = chart.getScaleX();
                if (Math.abs(newZoomLevel - currentZoomLevel) >= (newZoomLevel/10)) {
                    // Zoom level changed significantly
                    currentZoomLevel = newZoomLevel;

                    continuousmodeViewModel.setVisibleRangeStart((int) chart.getLowestVisibleX());
                    continuousmodeViewModel.setVisibleRangeEnd((int) chart.getHighestVisibleX());

                    updateChart(compressDataAndGetDataSet(continuousmodeViewModel.getVisibleRangeStart(), continuousmodeViewModel.getVisibleRangeEnd(), 1000));
                }
            }
            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                int visibleRangeStart = (int) chart.getLowestVisibleX();
                int visibleRangeEnd = (int) chart.getHighestVisibleX();
                continuousmodeViewModel.setVisibleRangeStart(visibleRangeStart);
                continuousmodeViewModel.setVisibleRangeEnd(visibleRangeStart);

                int span = visibleRangeEnd - visibleRangeStart;
                float translationThreshold = (float)span / 100; // Define an appropriate threshold value


                // Check if the chart is at its boundaries
                if ((visibleRangeStart<= chartMinX && dX > 0) || (visibleRangeEnd >= chartMaxX && dX < 0)) {
                    // At the boundary, do not update
                    return;
                }

                // Check if the translation is significant
                if (Math.abs(visibleRangeStart - prevRangeStart) > translationThreshold ||
                        Math.abs(visibleRangeEnd - prevRangeEnd) > translationThreshold &&
                                span >= 10 ) {

                    prevRangeStart = visibleRangeStart;
                    prevRangeEnd = visibleRangeEnd;

                    updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, 1000));
                }
            }
        });




        return root;
    }

    public void initChart() {
        // Configure the chart (optional, based on your needs)
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setScaleYEnabled(false); // Disable Y-axis scaling
        chart.setScaleXEnabled(true);  // Enable X-axis scaling

        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
        xAxis.setAxisMaximum(chartMaxX); // End at the maximum X value

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0); // Set minimum value for the left Y-axis
        leftAxis.setAxisMaximum(255); // Set maximum value for the left Y-axis

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinimum(0); // Set minimum value for the right Y-axis
        rightAxis.setAxisMaximum(255); // Set maximum value for the right Y-axis


    }


    private void refreshChart() {
        if(serialService.getRecordingContinuous()){
            // Get the current visible range
            int visibleRangeStart = (int) chart.getLowestVisibleX();
            int visibleRangeEnd = (int) chart.getHighestVisibleX();
            continuousmodeViewModel.setVisibleRangeStart((int) chart.getLowestVisibleX());
            continuousmodeViewModel.setVisibleRangeEnd((int) chart.getHighestVisibleX());

            chartMaxX = serialService.getBufferLength();
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX);
            xAxis.setAxisMaximum(chartMaxX);
            // Update the chart
            getActivity().runOnUiThread(() -> updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, 1000)));
        }
    }

    private LineDataSet compressDataAndGetDataSet(int rangeStart, int rangeEnd, int numberBins) {
        // Call the native method
        Object[] result = (Object[]) serialService.compressData(rangeStart, rangeEnd, numberBins);

        float[] timeValues = (float[]) result[0];
        float[] dataValues = (float[]) result[1];

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < timeValues.length; i++) {
            entries.add(new Entry(timeValues[i], dataValues[i]));
        }
        LineDataSet lineDataSet = new LineDataSet(entries, "Demodulator");
        // Disable the drawing of values for each point
        lineDataSet.setDrawValues(false);
        // Increase the line thickness (example: 3f for a thicker line)
        lineDataSet.setLineWidth(3f);
        // Set a more pronounced line color (example: Color.BLUE)
        lineDataSet.setColor(Color.parseColor("#003d99"));

        return lineDataSet;
    }

    private void updateChart(LineDataSet lineDataSet) {
        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void toggleVerticalLinesOnChart(long[] lineTimestamps) {
        XAxis xAxis = chart.getXAxis();

        if (!xAxis.getLimitLines().isEmpty()) {
            xAxis.removeAllLimitLines();
            Toast.makeText(getContext(), "Removed vertical lines", Toast.LENGTH_SHORT).show();
        } else {
            for (long timestamp : lineTimestamps) {
                LimitLine line = new LimitLine(timestamp);
                line.setLineColor(Color.RED);
                line.setLineWidth(2f);
                xAxis.addLimitLine(line);
            }
            Toast.makeText(getContext(), "Added " + lineTimestamps.length + " vertical lines", Toast.LENGTH_SHORT).show();
        }
    }




    private void unbindServiceIfNeeded() {
        if (isServiceBound && !isFragmentActive() && getActivity() != null) {
            getActivity().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private boolean isFragmentActive() {
        return isAdded() && !isDetached() && !isRemoving();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (!isServiceBound && getActivity() != null) {
            Intent intent = new Intent(getActivity(), SerialService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for avoiding memory leaks
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshChart, 0, refreshRate, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
        // Send the command
        if(isServiceBound){
            serialService.write(command);
        }

        long startTime = System.currentTimeMillis(); // Start time for timeout

        // Wait for the response with timeout
        while (isServiceBound && serialService.getBufferLength() < expectedResponseSize) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                return null; // Timeout occurred
            }
            try {
                Thread.sleep(busyDelay); // Wait for it to arrive
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // Retrieve the response
        byte[] response = new byte[expectedResponseSize];
        response = serialService.pollData(expectedResponseSize);

        serialService.clearBuffer(); // Optionally clear the queue after processing (pollData() should already clear the response)
        return response;
    }
}