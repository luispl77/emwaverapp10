package com.example.emwaver10.ui.packetmode;

import android.os.Bundle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.emwaver10.Constants;
import com.example.emwaver10.databinding.FragmentPacketModeBinding;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Queue;
import java.util.Arrays;

public class PacketModeFragment extends Fragment {

    private FragmentPacketModeBinding binding;

    private PacketModeViewModel packetModeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(this).get(PacketModeViewModel.class);

        binding = FragmentPacketModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        configTabLayout();

        return root;
    }

    private void configTabLayout(){
        ViewPagerAdapter adapter = new ViewPagerAdapter(requireActivity());
        binding.viewPager2.setAdapter(adapter);

        adapter.addFragment(new ReceiveFragment(), "RECEIVE");
        adapter.addFragment(new TransmitFragment(), "TRANSMIT");

        binding.viewPager2.setOffscreenPageLimit(adapter.getItemCount());

        TabLayoutMediator mediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager2, (tabLayout, position) -> {
            tabLayout.setText(adapter.getTitle(position));
        });

        mediator.attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        //IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        //requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers

        //IntentFilter filterBytes = new IntentFilter(Constants.ACTION_USB_DATA_BYTES_RECEIVED);
        //requireActivity().registerReceiver(usbDataReceiver, filterBytes);
    }

    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_USB_DATA_RECEIVED.equals(intent.getAction())) {
                String dataString = intent.getStringExtra("data");
                if (dataString != null) {
                    Log.i("ser string", dataString);
                }
            }
            else if (Constants.ACTION_USB_DATA_BYTES_RECEIVED.equals(intent.getAction())) {
                byte [] bytes = intent.getByteArrayExtra("bytes");
                if (bytes != null) {
                    // Optionally, you can log the byte array to see its contents
                    Log.i("ser bytes", Arrays.toString(bytes));
                    for (byte b : bytes) {
                        packetModeViewModel.addResponseByte(b);
                    }
                    Log.i("queue size", "" + packetModeViewModel.getResponseQueueSize());
                }
            }
        }
    };

    @Override
    public void onStop() {
        //requireActivity().unregisterReceiver(usbDataReceiver); //disable the routine for receiving data when we leave packet mode.
        super.onStop();
    }
}
