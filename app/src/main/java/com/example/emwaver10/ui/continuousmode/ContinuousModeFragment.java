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

import com.example.emwaver10.R;
import com.example.emwaver10.SerialService;
import com.example.emwaver10.databinding.FragmentContinuousModeBinding;
import com.example.emwaver10.databinding.FragmentScriptsBinding;
import com.example.emwaver10.ui.scripts.ScriptsViewModel;
import com.github.mikephil.charting.charts.LineChart;
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

public class ContinuousModeFragment extends Fragment {

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

    public ScheduledExecutorService scheduler;

    private final int refreshRate = 100; // Refresh rate in milliseconds





    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
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

        continuousmodeViewModel = new ViewModelProvider(this).get(ContinuousModeViewModel.class);

        binding.connectButton.setOnClickListener(v -> {
            serialService.connectUSBSerial();
            //updateVisibleRange();
        });

        binding.getBufferLengthButton.setOnClickListener(v -> {
            int bufferLength = serialService.getBufferLength();
            Toast.makeText(getContext(), "Buffer Length: " + bufferLength, Toast.LENGTH_SHORT).show();
            //refreshChart();
        });

        binding.clearBufferButton.setOnClickListener(v -> {
            serialService.clearBuffer();
            Toast.makeText(getContext(), "buffer cleared" , Toast.LENGTH_SHORT).show();
        });

        binding.startRecordingButton.setOnClickListener(v -> {
            String contCommand = "cont";
            byte[] byteArray = contCommand.getBytes();
            serialService.setRecordingContinuous(true);
            serialService.write(byteArray);
            serialService.setRecording(true);

        });

        binding.stopRecordingButton.setOnClickListener(v -> {
            String contCommand = "ssss";
            byte[] byteArray = contCommand.getBytes();
            serialService.write(byteArray);
            //serialService.setRecording(false);
            serialService.setRecordingContinuous(false);
            chartMaxX = serialService.getBufferLength();
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
            xAxis.setAxisMaximum(chartMaxX);
            updateChart(compressDataAndGetDataSet(0, serialService.getBufferLength(), 1000));
        });

        initChart();
        //refreshChart();


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
                if (Math.abs(newZoomLevel - currentZoomLevel) >= 0.5f && newZoomLevel < 200) {
                    // Zoom level changed significantly
                    currentZoomLevel = newZoomLevel;

                    int visibleRangeStart = (int) chart.getLowestVisibleX();
                    int visibleRangeEnd = (int) chart.getHighestVisibleX();

                    updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, 1000));
                }
            }
            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                int visibleRangeStart = (int) chart.getLowestVisibleX();
                int visibleRangeEnd = (int) chart.getHighestVisibleX();
                float translationThreshold = 500 / chart.getScaleX(); // Define an appropriate threshold value
                int span = visibleRangeEnd -visibleRangeStart;

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
        //Log.i("refresh", "refreshChart");
        if(serialService.getRecording()){
            // Get the current visible range
            int visibleRangeStart = (int) chart.getLowestVisibleX();
            int visibleRangeEnd = (int) chart.getHighestVisibleX();
            chartMaxX = serialService.getBufferLength();
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
            xAxis.setAxisMaximum(chartMaxX); //
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

}