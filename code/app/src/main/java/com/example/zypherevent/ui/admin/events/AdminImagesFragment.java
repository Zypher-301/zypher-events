package com.example.zypherevent.ui.admin.events;


import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zypherevent.model.AdminEvent;
import com.example.zypherevent.model.AdminImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminBaseListFragment
 * @see AdminImage
 * @see AdminImagesAdapter
 * @see "res/navigation/admin_navigation.xml"
 */


// Note: This needs its own Adapter (AdminImagesAdapter)
public class AdminImagesFragment extends AdminBaseListFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<AdminImage> images = new ArrayList<>();
        images.add(new AdminImage("amy.mcd@email.com", "23rd June, 2025"));
        images.add(new AdminImage("swim.center@email.com", "24th June, 2025"));

        AdminImagesAdapter adapter = new AdminImagesAdapter(images, image -> {
            Toast.makeText(getContext(), "Deleting image from " + image.getUploader(), Toast.LENGTH_SHORT).show();
            // TODO: Add Firebase delete logic
        });

        recyclerView.setAdapter(adapter);
    }
}