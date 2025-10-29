package com.example.zypherevent;

import android.os.Bundle;
import android.view.Menu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.zypherevent.databinding.AdminMainBinding;
import com.example.zypherevent.userTypes.User;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see res/layout/content_admin.xml
 * @see res/layout/admin_bar_main.xml
 * @see res/layout/admin_main.xml
 * @see res/navigation/admin_navigation.xml
 * @see res/menu/admin_main_drawer.xml
 */
public class AdminActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private AdminMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the admin layout using its binding class (admin_main.xml)
        binding = AdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the toolbar from the included admin_bar_main.xml (admin_main.xml)
        setSupportActionBar(binding.adminBarMain.toolbar);

        // Initialize DrawerLayout and NavigationView (admin_main.xml)
        DrawerLayout drawer = binding.adminDrawerLayout;
        NavigationView navigationView = binding.adminNavView;

        // Configure the top-level destinations for the admin drawer menu (admin_main_drawer.xml)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_events, R.id.nav_profile, R.id.nav_images, R.id.nav_notificationLog)
                .setOpenableLayout(drawer)
                .build();

        // Find the NavController using the ID from content_admin.xml (content_admin.xml)
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_admin);

        // Set up the ActionBar and NavigationView with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle the "Up" button by using the NavController (content_admin.xml)
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_admin);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}