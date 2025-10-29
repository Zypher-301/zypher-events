package com.example.zypherevent.ui.admin.events;


import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.model.AdminNotificationLog;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminBaseListFragment
 * @see AdminNotificationLog
 * @see AdminNotificationLogAdapter
 * @see "res/navigation/admin_navigation.xml"
 */

// Note: This needs its own Adapter (AdminNotificationLogAdapter)
public class AdminNotificationLogFragment extends AdminBaseListFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<AdminNotificationLog> logs = new ArrayList<>();
        logs.add(new AdminNotificationLog(
                "\"event start time has been changed\"",
                "Swim Class", "Accepted", "19 October 2025", "from Amy McDonald"
        ));
        logs.add(new AdminNotificationLog(
                "\"You won the lottery!\"",
                "Piano for Beginners", "Selected", "18 October 2025", "from System"
        ));

        AdminNotificationLogAdapter adapter = new AdminNotificationLogAdapter(logs);
        recyclerView.setAdapter(adapter);
    }
}
