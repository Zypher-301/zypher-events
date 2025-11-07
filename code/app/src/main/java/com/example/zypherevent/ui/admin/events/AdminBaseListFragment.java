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
 * An abstract base class for fragments that display a list of items within the admin section of the application.
 * <p>
 * This class provides common functionality for admin-related list views, such as inflating a shared layout
 * containing a {@link RecyclerView} and setting up the {@link LinearLayoutManager}. Subclasses are responsible
 * for providing the specific data and adapter for the {@code RecyclerView}.
 * <p>
 * The layout used by this fragment is {@code R.layout.fragment_admin_fragment_list_page}.
 *
 * @see Fragment
 * @see RecyclerView
 */

public abstract class AdminBaseListFragment extends Fragment {

    protected RecyclerView recyclerView;

    /**
     * Called to have the fragment instantiate its user interface view.
     * <p>
     * This method inflates the shared layout for all admin list fragments,
     * {@code R.layout.fragment_admin_fragment_list_page}. This consistent layout
     * provides a {@link RecyclerView} for displaying lists of data.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     *                           The fragment should not add the view itself, but this can be used to generate
     *                           the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return Returns the inflated {@link View} for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // All admin fragments use the same list page layout
        return inflater.inflate(R.layout.fragment_admin_fragment_list_page, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored into the view.
     * This method initializes the {@link RecyclerView} for the list display.
     * <p>
     * It finds the {@code RecyclerView} by its ID from the inflated view and sets a
     * {@link LinearLayoutManager}. Subclasses are expected to set their own adapters
     * on the {@code recyclerView} field.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}