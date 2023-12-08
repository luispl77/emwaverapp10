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
        Log.i("onDestroy", "on start");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("onStart", "on start");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("onStop", "on stop");
    }
}
