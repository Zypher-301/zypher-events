package com.example.zypherevent.ui.admin.events;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
import com.example.zypherevent.model.AdminEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminEvent
 * @see AdminBaseListFragment
 * @see AdminEventsAdapter
 */

// Note: This needs its own Adapter (AdminEventsAdapter)
public class AdminEventsFragment extends AdminBaseListFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<AdminEvent> events = new ArrayList<>();
        events.add(new AdminEvent("Beginner Swim Lessons", "Mondays, 5-6 PM", "Lottery closes Dec 15"));
        events.add(new AdminEvent("Interpretive Dance Class", "Tuesdays, 7-9 PM", "20 spots left"));
        events.add(new AdminEvent("Piano for Beginners", "Saturdays, 10-11 AM", "Full"));

        AdminEventsAdapter adapter = new AdminEventsAdapter(events, event -> {
            Toast.makeText(getContext(), "Deleting " + event.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Add Firebase delete logic
        });

        recyclerView.setAdapter(adapter);
    }
}