package com.icarus.events;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.res.ResourcesCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Activity that allows organizers to create a new event.
 * <p>
 * Users can enter event details such as name, category, location,
 * registration period, event date, participant limit, and whether
 * geolocation tracking is enabled. Event information is validated
 * and then stored in the Firestore "events" collection.
 * <p>
 * This activity extends {@link HeaderNavBarActivity} to include
 * the application's reusable navigation bar.
 *
 * @author Ben Salmon
 */
public class OrganizerCreateEventActivity extends HeaderNavBarActivity {
    private  FirebaseFirestore db;

    private ImageView eventPoster;
    private Button UploadPosterButton;

    private EditText eventName;
    private EditText eventDescription;

    private EditText locationName;
    private SwitchMaterial geolocationSwitch;
    private SwitchMaterial privateSwitch;
    private EditText EventLimit;
    private Spinner categoryNameList;

    private Button RegistrationStartDateButton;
    private Button RegistrationStartTimeButton;
    private Button RegistrationEndDateButton;
    private Button RegistrationEndTimeButton;

    private Button eventStartDate, eventEndDate;
    private Button eventStartTime, eventEndTime;
    private Button CreateEvent;

    private Date startDate;
    private Date endDate;
    private Date eventStartDateDate, eventEndDateDate;

    private Date startTime;
    private Date endTime;
    private Date eventStartTimeTime, eventEndTimeTime;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private String posterURL;
    private Uri posterURI;
    private GeoPoint selectedEventLocation = null;
    private Integer selectedEntrantRange = null;
    private Polygon entrantRangeCircle = null;

    /**
     * Initializes the event creation interface.
     * <p>
     * This method sets up all UI components, retrieves event categories
     * from Firestore, and assigns click listeners for selecting dates
     * and submitting the event. When the user confirms creation, the
     * event data is validated and saved to the database.
     *
     * @param savedInstanceState the previously saved activity state,
     *                           or null if the activity is starting fresh
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);
        setupNavBar();
        User user = UserSession.getInstance().getCurrentUser();
        String userId = user.getId();
        db = FirebaseFirestore.getInstance();

        //Create Spinner
        categoryNameList = findViewById(R.id.OrganizerCreateEventCategory);
        ArrayList<String> dbCategories = new ArrayList<>();
        db.collection(FirestoreCollections.EVENT_CATEGORIES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    dbCategories.add("Category");
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        String category = doc.getString("category");
                        //check for null
                        if(category != null){
                            dbCategories.add(category);
                        }
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                            this,
                            R.layout.organizer_create_event_spinner_content,
                            dbCategories
                    );
                    categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categoryNameList.setAdapter(categoryAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        //Create Image Picker Launcher an initialize posterURI
        posterURI = null;
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        posterURI = uri;
                        eventPoster.setImageURI(uri);
                    }
                }
        );


        //Create ImageView
        eventPoster = findViewById(R.id.OrganizerCreateEventImage);

        //Create EditText
        eventName = findViewById(R.id.OrganizerCreateEventEventTitle);
        eventDescription = findViewById(R.id.OrganizerCreateEventDescription);
        EventLimit= findViewById(R.id.OrganizerCreateEventLimitWaitingListLimit);
        locationName = findViewById(R.id.OrganizerCreateEventEventLocation);

        //Create Switch
        geolocationSwitch = findViewById(R.id.OrganizerCreateEventGeolocationSwitch);
        privateSwitch = findViewById(R.id.OrganizerCreateEventPrivateSwitch);
        //Create Buttons
        UploadPosterButton = findViewById(R.id.OrganizerCreateEventUploadPosterButton);

        RegistrationStartDateButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStartDate);
        RegistrationStartTimeButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodStartTime);

        RegistrationEndDateButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEndDate);
        RegistrationEndTimeButton = findViewById(R.id.OrganizerCreateEventRegistrationPeriodEndTime);

        eventStartDate = findViewById(R.id.OrganizerCreateEventStartDate);
        eventStartTime = findViewById(R.id.OrganizerCreateEventStartTime);
        eventEndDate = findViewById(R.id.OrganizerCreateEventEndDate);
        eventEndTime = findViewById(R.id.OrganizerCreateEventEndTime);

        CreateEvent = findViewById(R.id.OrganizerCreateEventCreateEvent);


        //Button Functions
        UploadPosterButton.setOnClickListener(v -> {
            //Upload event poster
            imagePickerLauncher.launch("image/*");
        });

        // When geolocation is enabled popup map to set event location
        geolocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showEventLocationPickerDialog();
            } else {
                selectedEventLocation = null;
            }
        });

        RegistrationStartDateButton.setOnClickListener(v -> {
            // Set Registration start date
            showDatePicker(date -> {
                this.startDate = date;
                String regStart = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationStartDateButton.setText("Start Date:\n"+ regStart);
            });
        });
        RegistrationStartTimeButton.setOnClickListener(v -> {
            // Set Registration start Time
            showTimePicker(time->{
                startTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(startTime);
                RegistrationStartTimeButton.setText("Start Time:\n"+showTime );
            });
        });

        RegistrationEndDateButton.setOnClickListener(v -> {
            // Set Registration end date
            showDatePicker(date -> {
                this.endDate = date;
                String regEnd = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                RegistrationEndDateButton.setText("End Date:\n"+ regEnd);
            });
        });
        RegistrationEndTimeButton.setOnClickListener(v -> {
            // Set Registration end Time
            showTimePicker(time->{
                endTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(endTime);
                RegistrationEndTimeButton.setText("End Time:\n"+showTime );
            });
        });

        eventStartDate.setOnClickListener(v -> {
            // Set event date
            showDatePicker(date -> {
                this.eventStartDateDate = date;
                String eventDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                eventStartDate.setText("Event Date:\n"+ eventDate);
            });
        });
        eventStartTime.setOnClickListener(v -> {
            // Set Registration end Time
            showTimePicker(time->{
                eventStartTimeTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(time);
                eventStartTime.setText("Event Time:\n"+showTime );
            });
        });
        eventEndDate.setOnClickListener(v -> {
            // Set event date
            showDatePicker(date -> {
                this.eventEndDateDate = date;
                String eventDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date);
                eventEndDate.setText("Event Date:\n"+ eventDate);
            });
        });
        eventEndTime.setOnClickListener(v -> {
            // Set Registration end Time
            showTimePicker(time->{
                eventEndTimeTime = time;
                String showTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(time);
                eventEndTime.setText("Event Time:\n"+showTime );
            });
        });

        CreateEvent.setOnClickListener(v -> {
            // Confirm creation of event
            String name = eventName.getText().toString().trim();
            String description = eventDescription.getText().toString().trim();
            String category = categoryNameList.getSelectedItem().toString().trim();
            String location = locationName.getText().toString().trim();
            ArrayList<String> organizerIds = new ArrayList<>();
            organizerIds.add(userId);


            // Check if user filled in all dates before proceeding
            if (startDate == null || startTime == null || endDate == null || endTime == null ||
                    eventStartDateDate == null || eventStartTimeTime == null ||
                    eventEndDateDate == null || eventEndTimeTime == null) {
                Toast.makeText(this, "Please select all dates", Toast.LENGTH_SHORT).show();
                return;
            }

            this.startDate = mergeDateAndTime(startDate,startTime);
            this.endDate = mergeDateAndTime(endDate,endTime);
            this.eventStartDateDate = mergeDateAndTime(eventStartDateDate,eventStartTimeTime);
            this.eventEndDateDate = mergeDateAndTime(eventEndDateDate,eventEndTimeTime);

            if (startDate.before(new Date())) {
                Toast.makeText(this, "Registration Start Date cannot be in the past.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(startDate.after(endDate)){
                Toast.makeText(this, "Registration Start Date cannot be after Registration End Date.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(endDate.after(eventStartDateDate)){
                Toast.makeText(this, "Registration End Date cannot be after Event Start Date.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(eventStartDateDate.after(eventEndDateDate)){
                Toast.makeText(this, "Event Start Date cannot be after Event End Date.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if user filled all text fields before proceeding
            if (name.isEmpty() || category.equals("Category") || location.isEmpty()) {
                Toast.makeText(this, "Please fill in name, category, and location fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            //Uppercase first letter and lowercase rest of string
            Double numberOfPeople = null;
            if (!EventLimit.getText().toString().trim().isEmpty()) {
                numberOfPeople = Double.parseDouble(EventLimit.getText().toString().trim());
            }
            location = location.substring(0,1).toUpperCase() + location.substring(1).toLowerCase();

            // Upload image to cloudinary and save event to database
            final String finalName = name;
            final String finalDescription = description;
            final String finalCategory = category;
            final String finalLocation = location;
            final Double finalCapacity = numberOfPeople;
            final ArrayList<String> finalOrganizerIds = organizerIds;
            if (posterURI != null) {
                MediaManager.get().upload(posterURI)
                        .option("upload_preset", "ml_default")
                        .callback(new UploadCallback() {
                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                posterURL = (String) resultData.get("secure_url");
                                String publicId = (String) resultData.get("public_id");
                                // Create new firebase document for the image
                                Map<String, Object> imageData = new HashMap<>();
                                imageData.put("URL", posterURL);
                                db.collection(FirestoreCollections.IMAGES_COLLECTION)
                                        .document(publicId)
                                        .set(imageData)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(OrganizerCreateEventActivity.this,
                                                    "Image uploaded", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(OrganizerCreateEventActivity.this,
                                                    "Image upload failed", Toast.LENGTH_SHORT).show();
                                        });
                                // Save event to firestore
                                saveEvent(finalName, finalDescription, finalCategory, finalCapacity, posterURL, finalLocation, finalOrganizerIds);
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                posterURL = "";
                                Toast.makeText(OrganizerCreateEventActivity.this,
                                        "Image upload failed", Toast.LENGTH_SHORT).show();
                                Log.e("UPLOAD_ERROR", error.getDescription());
                                // Save event to firestore with empty poster
                                saveEvent(finalName, finalDescription,finalCategory, finalCapacity, posterURL, finalLocation, finalOrganizerIds);
                            }

                            @Override public void onStart(String requestId) {}
                            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                            @Override public void onReschedule(String requestId, ErrorInfo error) {}
                        })
                        .dispatch();
            } else {
                // Save event to firestore with empty poster
                saveEvent(finalName, finalDescription, finalCategory, finalCapacity, "", finalLocation, finalOrganizerIds);
            }
        });
    }

    /**
     * Saves event information to a new Firestore event document.
     *
     * @param name          Name of the event
     * @param description   Event description
     * @param category      Category of the event
     * @param capacity      Waitlist capacity of the event
     * @param posterURL     URL of the events poster
     * @param location      Event location coordinates (null if geolocation turned off)
     * @param organizerIds  Array of user document ID's of users with event "organizer" permissions
     */
    private void saveEvent(String name, String description, String category, Double capacity, String posterURL, String location, ArrayList<String> organizerIds) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", name);
        eventData.put("description", description);
        eventData.put("category", category);
        eventData.put("capacity",capacity);
        eventData.put("open", startDate);
        eventData.put("close", endDate);
        eventData.put("startDate", eventStartDateDate);
        eventData.put("endDate", eventEndDateDate);
        eventData.put("image", posterURL);
        eventData.put("location", location);
        eventData.put("geolocation",geolocationSwitch.isChecked());
        eventData.put("isPrivate", privateSwitch.isChecked());
        eventData.put("organizers", organizerIds);
        eventData.put("entrantRange",selectedEntrantRange);
        if (selectedEventLocation != null) {
            com.google.firebase.firestore.GeoPoint firestoreLocation =
                    new com.google.firebase.firestore.GeoPoint(
                            selectedEventLocation.getLatitude(),
                            selectedEventLocation.getLongitude()
                    );
            eventData.put("coordinates", firestoreLocation);
        } else {
            eventData.put("coordinates", null);
        }

        //Event event = new Event(null,name,category,numberOfPeople, this.startDate,this.endDate,this.eventDate);
        db.collection(FirestoreCollections.EVENTS_COLLECTION).add(eventData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Event created", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays a material design date picker and returns the selected date.
     * <p>
     * The picker restricts selections to current or future dates. The
     * selected date is converted from UTC to the device's local time zone
     * before being returned through the provided callback.
     * <p>
     * This was created by claude AI, March 10, 2026 "How can I create a
     * popup calendar without creating a new XML file"
     *
     * @param onDatePicked callback executed when the user selects a date
     */
    private void  showDatePicker(Consumer<Date> onDatePicked){
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);

            Calendar local = Calendar.getInstance();
            local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            local.set(Calendar.MILLISECOND, 0);

            onDatePicked.accept(local.getTime());
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    /**
     * Displays a material design time picker and returns the selected time.
     * <p>
     * The picker uses a clock input mode and returns the selected hour and
     * minute through the provided callback as a Date object set to today's
     * date at the chosen time in the device's local time zone.
     * <p>
     * This was created by claude AI, March 22, 2026 "How can I create a
     * popup clock without creating a new XML file"
     *
     * @param onTimePicked callback executed when the user selects a time
     */
    private void showTimePicker(Consumer<Date> onTimePicked) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            Calendar local = Calendar.getInstance();
            local.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            local.set(Calendar.MINUTE, timePicker.getMinute());
            local.set(Calendar.SECOND, 0);
            local.set(Calendar.MILLISECOND, 0);

            onTimePicked.accept(local.getTime());
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    /**
     * Merges the date components from one Date and the time components
     * from another into a single combined Date object.
     *<p>
     * @param date the Date to take year, month, and day from
     * @param time the Date to take hour and minute from
     * @return a new Date combining both, or null if either is null
     *<p>
     * This was created by claude AI, March 22, 2026 "How can I combine the Date
     * and Time from 2 different Date objects"
     */
    private Date mergeDateAndTime(Date date, Date time) {
        if (date == null || time == null) return null;

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(time);

        dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        dateCal.set(Calendar.MINUTE,      timeCal.get(Calendar.MINUTE));
        dateCal.set(Calendar.SECOND, 0);
        dateCal.set(Calendar.MILLISECOND, 0);

        return dateCal.getTime();
    }

    /**
     * Sets up Dialog to let organizers select the event location on a map
     * and set the area entrants must join from. Fires when the geolcation switch is flipped.
     */
    private void showEventLocationPickerDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_event_location_picker);

        MapView dialogMap = dialog.findViewById(R.id.event_location_picker_map);
        TextView eventLocationCoordinates = dialog.findViewById(R.id.event_location_picker_coordinates);
        SeekBar entrantRangeSlider = dialog.findViewById(R.id.event_location_picker_slider);
        TextView entrantRangeValue = dialog.findViewById(R.id.event_location_picker_slider_value);
        Button confirmButton = dialog.findViewById(R.id.event_location_picker_confirm_button);
        Button cancelButton = dialog.findViewById(R.id.event_location_picker_cancel_button);

        // Set up map
        Configuration.getInstance().setUserAgentValue(getPackageName());
        dialogMap.setTileSource(TileSourceFactory.MAPNIK);
        dialogMap.setMultiTouchControls(true);
        dialogMap.getController().setZoom(12.0);

        // Default center (Edmonton) or last selected
        GeoPoint startPoint = selectedEventLocation != null
                ? selectedEventLocation
                : new GeoPoint(53.5461, -113.4938);
        dialogMap.getController().setCenter(startPoint);

        // Marker for selected point
        Marker[] selectedMarker = {null};

        // Tap to place marker
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Remove old marker
                if (selectedMarker[0] != null) {
                    dialogMap.getOverlays().remove(selectedMarker[0]);
                }
                // Place new marker
                Marker marker = new Marker(dialogMap);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setIcon(ResourcesCompat.getDrawable(getResources(), android.R.drawable.btn_star_big_on, getTheme()));
                dialogMap.getOverlays().add(marker);
                dialogMap.invalidate();
                selectedMarker[0] = marker;
                if (selectedEntrantRange != null && selectedEntrantRange > 0) {
                    dialogMap.getOverlays().remove(entrantRangeCircle);
                    entrantRangeCircle = drawCircle(p.getLatitude(), p.getLongitude(), selectedEntrantRange * 1000, dialogMap);
                }

                // Update label
                String coords = String.format(Locale.getDefault(),
                        "%.4f, %.4f", p.getLatitude(), p.getLongitude());
                eventLocationCoordinates.setText(coords);

                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        });

        dialogMap.getOverlays().add(0, eventsOverlay);

        // Setup slider
        entrantRangeSlider.setMax(100);
        entrantRangeSlider.setProgress(0);
        entrantRangeValue.setText("0 km");
        entrantRangeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                entrantRangeValue.setText(progress + " km");
                if (selectedMarker[0] != null) {
                    if (entrantRangeCircle != null) {
                        dialogMap.getOverlays().remove(entrantRangeCircle);
                        entrantRangeCircle = null;
                    }
                    if (progress > 0) {
                        GeoPoint position = selectedMarker[0].getPosition();
                        entrantRangeCircle = drawCircle(position.getLatitude(), position.getLongitude(), progress * 1000, dialogMap);
                    }
                    dialogMap.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() > 0) {
                    selectedEntrantRange = seekBar.getProgress();
                }
            }
        });

        confirmButton.setOnClickListener(v -> {
            if (selectedMarker[0] == null) {
                Toast.makeText(this, "Please tap a location on the map", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedEntrantRange == null) {
                Toast.makeText(this, "Please select an entrant range", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedEventLocation = selectedMarker[0].getPosition();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            geolocationSwitch.setChecked(false); // revert switch if cancelled
            selectedEntrantRange = 0;
            dialog.dismiss();
        });

        // Also revert switch if dialog is dismissed without confirming
        dialog.setOnCancelListener(d -> geolocationSwitch.setChecked(false));

        dialog.show();
        dialogMap.onResume();
    }

    /**
     * Draws a circle on a given map view.
     *
     * @param lat           Latitude of the circle centre
     * @param lon           Longitude of hte circle centre
     * @param radiusMeters  Radius of the circle in meters
     * @param map           Map view to draw the circle on
     * @return              Polygon object reference for deleting the circle from the map.
     */
    // Created by Claude AI, March 28, 2026
    // "How to draw a circle around a point on the map using the osmdroid library in java"
    private Polygon drawCircle(double lat, double lon, double radiusMeters, MapView map) {
        Polygon circle = new Polygon();
        circle.setPoints(Polygon.pointsAsCircle(new GeoPoint(lat, lon), radiusMeters));

        // Style the circle
        circle.getFillPaint().setColor(0x300078FF);   // semi-transparent blue fill (ARGB)
        circle.getOutlinePaint().setColor(0xFF0078FF); // solid blue border
        circle.getOutlinePaint().setStrokeWidth(3f);

        map.getOverlays().add(circle);
        return circle;
    }
}

