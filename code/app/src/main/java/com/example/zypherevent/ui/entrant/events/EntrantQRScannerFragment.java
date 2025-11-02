package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zypherevent.R;

public class EntrantQRScannerFragment extends Fragment {

    public EntrantQRScannerFragment() {
        // public no-arg constructor required
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_qr_scanner, container, false);
    }
}
