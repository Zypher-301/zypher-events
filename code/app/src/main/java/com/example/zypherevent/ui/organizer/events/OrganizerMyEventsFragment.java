package com.example.zypherevent.ui.organizer.events;

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
 * Basic Organizer "My Events" fragment: inflates fragment_organizer_my_events layout.
 */
public class OrganizerMyEventsFragment extends Fragment {

    public OrganizerMyEventsFragment() {
        // public no-arg constructor required
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the simple Organizer "My Events" layout
        return inflater.inflate(R.layout.fragment_organizer_my_events, container, false);
    }
}
