package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity class that provides a reusable navigation bar for subclasses.
 * <p>
 * Subclasses should call setupNavBar() after setContentView() in their onCreate()
 * to initialize the navigation bar's click listeners.
 *
 * @author Bradley Bradley
 */
public class HeaderNavBarActivity extends AppCompatActivity {


    /**
     * Initializes the base navigation bar activity.
     *
     * @param savedInstanceState the previously saved activity state, or null if
     *                           the activity is being created for the first time
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    /**
     * Initializes the navigation bar and assigns click listeners to its buttons.
     * <p>
     * Each navigation item launches the corresponding activity when selected.
     * This method should be called by subclasses after setting their layout
     * with setContentView().
     */
    protected void setupNavBar() {
        View navBar = findViewById(R.id.nav_bar);

        // Profile will be a class named "UserRegistrationActivity"
        navBar.findViewById(R.id.profile)
                .setOnClickListener((v -> openActivity(UserProfileActivity.class)));
        navBar.findViewById(R.id.event_details)
                .setOnClickListener((v -> openActivity(EntrantEventListActivity.class)));
        navBar.findViewById(R.id.registered_events)
                .setOnClickListener((v -> openActivity(EventHistoryActivity.class)));
    }


    protected void setupHeaderBar(String activityTitle) {
        View headerBar = findViewById(R.id.header_bar);

        headerBar.findViewById(R.id.back_button)
                .setOnClickListener(v -> finish());
        ((TextView) headerBar.findViewById(R.id.header_title)).setText(activityTitle);
    }

    /**
     * Launches the specified activity if it is not already the current activity.
     * <p>
     * The transition animation is disabled to provide seamless navigation
     * between screens using the navigation bar.
     *
     * @param cls the activity class to launch
     */
    private void openActivity(Class<?> cls) {
        if (!this.getClass().equals(cls)) {
            Intent intent = new Intent(this, cls);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // Turn off transition
            startActivity(intent);

        }
    }
}


/*
USAGE EXAMPLE:

public class HomeActivity extends NavigationBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupNavBar();
    }
}
*/