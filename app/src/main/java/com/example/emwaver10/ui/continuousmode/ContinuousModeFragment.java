package com.example.emwaver10.ui.continuousmode;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emwaver10.R;

public class ContinuousModeFragment extends Fragment {

    private ContinuousModeViewModel mViewModel;

    public static ContinuousModeFragment newInstance() {
        return new ContinuousModeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_continuous_mode, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ContinuousModeViewModel.class);
        // TODO: Use the ViewModel
    }

}