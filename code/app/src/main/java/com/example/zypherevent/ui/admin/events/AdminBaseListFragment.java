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

    /**
     * Inflates the layout for this fragment.
     * <p>
     * This method is called to have the fragment instantiate its user interface view.
     * All admin list fragments share the same layout file, {@code R.layout.fragment_admin_fragment_list_page},
     * which is inflated here.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     *                  The fragment should not add the view itself, but this can be used to generate
     *                  the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // All admin fragments use the same list page layout
        return inflater.inflate(R.layout.fragment_admin_fragment_list_page, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     * <p>
     * In this base implementation, it finds the RecyclerView from the inflated view
     * and sets up its LayoutManager.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given in the Bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the RecyclerView and set it up
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}