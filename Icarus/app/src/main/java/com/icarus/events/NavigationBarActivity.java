package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity class that provides a reusable navigation bar for subclasses.
 *
 * Subclasses should call setupNavBar() after setContentView() in their onCreate()
 * to wire up the navigation bar's click listeners.
 *
 * @author Bradley Bradley
 */
public class NavigationBarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // TODO: adjust where the profile and registered event click listeners point
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

    private void openActivity(Class<?> cls) {
        if (!this.getClass().equals(cls)) {
            Intent intent = new Intent(this, cls);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // Turn off transition
            startActivity(intent);

        }
    }
}


/*
public class HomeActivity extends NavigationBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupNavBar();
    }
}
 */