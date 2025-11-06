package com.example.zypherevent.ui.admin.events;


import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zypherevent.model.AdminImage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        SimpleDateFormat formatter = new SimpleDateFormat("d'st' MMMM, yyyy", Locale.ENGLISH);

        try {
            images.add(new AdminImage("img1", "http://example.com/amy.jpg", "amy.mcd@email.com", formatter.parse("23rd June, 2025")));
            images.add(new AdminImage("img2", "http://example.com/swim.jpg", "swim.center@email.com", formatter.parse("24th June, 2025")));
        } catch (ParseException e) {
            // TODO: Handle exception, perhaps show an error message to the user
            e.printStackTrace();
        }


        AdminImagesAdapter adapter = new AdminImagesAdapter(images, image -> {
            Toast.makeText(getContext(), "Deleting image from " + image.getUploader(), Toast.LENGTH_SHORT).show();
            // TODO: Add Firebase delete logic
        });

        recyclerView.setAdapter(adapter);
    }
}
