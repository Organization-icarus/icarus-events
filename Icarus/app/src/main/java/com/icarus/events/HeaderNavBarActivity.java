package com.icarus.events;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base activity class that provides a reusable navigation bar for subclasses.
 * <p>
 * Subclasses should call setupNavBar() after setContentView() in their onCreate()
 * to initialize the navigation bar's click listeners.
 *
 * @author Bradley Bradley
 */
public class HeaderNavBarActivity extends AppCompatActivity {

    protected static final int TAB_NONE = -1;
    protected static final int TAB_REGISTERED = 0;
    protected static final int TAB_EVENTS = 1;
    protected static final int TAB_PROFILE = 2;


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
    protected void setupNavBar(int activeTab) {
        View navBar = findViewById(R.id.nav_bar);

        // Profile will be a class named "UserRegistrationActivity"
        View registeredTab = navBar.findViewById(R.id.registered_events);
        View eventsTab = navBar.findViewById(R.id.event_details);
        View profileTab = navBar.findViewById(R.id.profile);

        registeredTab.setOnClickListener(v -> openActivity(EventHistoryActivity.class));
        eventsTab.setOnClickListener(v -> openActivity(EntrantEventListActivity.class));
        profileTab.setOnClickListener(v -> openActivity(UserProfileActivity.class));

        highlightNavTab(navBar, activeTab);
    }


    private void highlightNavTab(View navBar, int activeTab) {
        ImageView registeredIcon = navBar.findViewById(R.id.registered_icon);
        TextView registeredText = navBar.findViewById(R.id.registered_text);

        ImageView eventsIcon = navBar.findViewById(R.id.events_icon);
        TextView eventsText = navBar.findViewById(R.id.events_text);

        ImageView profileIcon = navBar.findViewById(R.id.profile_icon);
        TextView profileText = navBar.findViewById(R.id.profile_text);

        int activeColor = getColor(R.color.accent_first);
        int inactiveColor = getColor(R.color.white);

        registeredIcon.setColorFilter(activeTab == TAB_REGISTERED ? activeColor : inactiveColor);
        registeredText.setTextColor(activeTab == TAB_REGISTERED ? activeColor : inactiveColor);

        eventsIcon.setColorFilter(activeTab == TAB_EVENTS ? activeColor : inactiveColor);
        eventsText.setTextColor(activeTab == TAB_EVENTS ? activeColor : inactiveColor);

        profileIcon.setColorFilter(activeTab == TAB_PROFILE ? activeColor : inactiveColor);
        profileText.setTextColor(activeTab == TAB_PROFILE ? activeColor : inactiveColor);
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

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
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