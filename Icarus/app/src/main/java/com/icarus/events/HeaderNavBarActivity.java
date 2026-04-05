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
 * Base activity that provides a reusable header and bottom navigation bar.
 * <p>
 * Subclasses are expected to call {@link #setupNavBar(int)} and optionally
 * {@link #setupHeaderBar(String)} after {@code setContentView()} to initialize
 * navigation behavior and UI state.
 *
 * @author Bradley Bravender
 */
public class HeaderNavBarActivity extends AppCompatActivity {

    protected static final int TAB_NONE = -1;
    protected static final int TAB_REGISTERED = 0;
    protected static final int TAB_EVENTS = 1;
    protected static final int TAB_PROFILE = 2;

    /**
     * Initializes the activity.
     *
     * @param savedInstanceState the previously saved state, or null if none
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Sets up the navigation bar, assigns click listeners, and highlights
     * the active tab.
     * <p>
     * Each tab navigates to its corresponding activity. The Events tab
     * clears intermediate activities from the back stack.
     *
     * @param activeTab the currently active tab (one of the TAB_* constants)
     */
    protected void setupNavBar(int activeTab) {
        View navBar = findViewById(R.id.nav_bar);

        // Profile will be a class named "UserRegistrationActivity"
        View registeredTab = navBar.findViewById(R.id.registered_events);
        View eventsTab = navBar.findViewById(R.id.event_details);
        View profileTab = navBar.findViewById(R.id.profile);

        registeredTab.setOnClickListener(v -> openActivity(EventHistoryActivity.class));
        eventsTab.setOnClickListener(v -> openEventsRoot());
        profileTab.setOnClickListener(v -> openActivity(UserProfileActivity.class));

        highlightNavTab(navBar, activeTab);
    }

    /**
     * Launches the root Events screen and clears intermediate activities
     * from the back stack if present.
     */
    private void openEventsRoot() {
        Intent intent = new Intent(this, EntrantEventListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    /**
     * Updates the navigation bar UI to reflect the active tab.
     *
     * @param navBar the navigation bar view
     * @param activeTab the currently active tab
     */
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

    /**
     * Configures the header bar with a title and back navigation behavior.
     *
     * @param activityTitle the title displayed in the header
     */
    protected void setupHeaderBar(String activityTitle) {
        View headerBar = findViewById(R.id.header_bar);

        headerBar.findViewById(R.id.back_button)
                .setOnClickListener(v -> finish());
        ((TextView) headerBar.findViewById(R.id.header_title)).setText(activityTitle);
    }

    /**
     * Launches the specified activity if it is not already active.
     * <p>
     * Prevents redundant launches of the current activity.
     *
     * @param cls the activity class to launch
     */
    private void openActivity(Class<?> cls) {
        if (!this.getClass().equals(cls)) {
            Intent intent = new Intent(this, cls);
            // intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // Turn off transition
            startActivity(intent);

        }
    }

    /**
     * Converts density-independent pixels (dp) to raw pixels.
     *
     * @param dp the value in dp
     * @return the equivalent value in pixels
     */
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

        setupNavBar(TAB_NONE);
    }
}
*/