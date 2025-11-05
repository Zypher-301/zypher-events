package com.example.zypherevent.ui.organizer.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;

/**
 * @author Elliot Chrystal
 *
 * @version 1.0
 */
public class OrganizerMyEventsFragment extends Fragment {

    public OrganizerMyEventsFragment() {
        // public no-arg constructor required
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_my_events, container, false);
    }
}
