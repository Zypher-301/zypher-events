package com.example.zypherevent.model;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.zypherevent.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class LabelInfoWindow extends InfoWindow {

    public LabelInfoWindow(MapView mapView) {
        super(R.layout.marker_info_window, mapView);
    }

    @Override
    public void onOpen(Object item) {
        if (!(item instanceof Marker)) return;

        Marker marker = (Marker) item;
        View view = mView;
        if (view == null) return;

        TextView titleView = view.findViewById(R.id.bubble_title);
        if (titleView != null) {
            @Nullable String title = marker.getTitle();
            titleView.setText(title != null ? title : "");
        }
    }

    @Override
    public void onClose() { }
}