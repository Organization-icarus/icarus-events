package com.icarus.events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Instrumented UI tests for QR code scanning functionality.
 * <p>
 * This test verifies that the QR scanner can be opened from the
 * entrant event list screen and that the scanner UI is displayed.
 *
 * User Stories Tested:
 *     US 01.06.01 – As an entrant I want to view event details
 *     within the app by scanning the promotional QR code.
 *
 * Author: Alex Alves
 */
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class QRCodeScanTest {

    /**
     * Minimal test to verify that pressing the QR scan button
     * in the event list opens the QRCodeActivity with the scanner open.
     *
     * User Story Tested:
     *     US 01.06.01 – As an entrant I want to view event details
     *     within the app by scanning the promotional QR code.
     */
    @Test
    public void testOpenQrScannerFromEventList() {

        // Launch event list screen
        ActivityScenario.launch(EntrantEventListActivity.class);

        // Click QR scan button (update ID if needed)
        onView(withId(R.id.entrant_event_list_qr_button)).perform(click());

        // Verify scanner view is displayed (from QRCodeActivity)
        onView(withId(R.id.camera_preview))
                .check(matches(isDisplayed()));
    }
}