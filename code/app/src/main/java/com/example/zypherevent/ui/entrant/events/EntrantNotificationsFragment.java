package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zypherevent.R;

/**
 * @author Elliot Chrystal
 *
 * @version 1.0
 */
public class EntrantNotificationsFragment extends Fragment {

    public EntrantNotificationsFragment() {
        // public no-arg constructor required
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_notifications, container, false);
    }
}