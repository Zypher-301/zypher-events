package com.example.zypherevent.ui.admin.events;

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
 * @author Arunavo Dutta
 * @version 1.0
 * @see res/layout/fragment_admin_fragment_list_page.xml
 * A base fragment for all admin list pages.
 * It inflates the list layout and sets up the RecyclerView.
 */

public abstract class AdminBaseListFragment extends Fragment {

    protected RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // All admin fragments use the same list page layout
        return inflater.inflate(R.layout.fragment_admin_fragment_list_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the RecyclerView and set it up
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}