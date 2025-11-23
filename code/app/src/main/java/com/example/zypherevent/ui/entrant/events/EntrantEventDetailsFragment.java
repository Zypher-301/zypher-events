package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;


/**
 * @author Elliot Chrystal
 * @version 1.0
 *
 * Fragment that displays detailed information about a single {@link Event}.
 * Shows the event poster, metadata, registration window, and waitlist count
 * without exposing individual entrant identities.
 */
public class EntrantEventDetailsFragment extends Fragment {

    /** Argument key for passing an Event into this fragment. */
    public static final String ARG_EVENT = "arg_event";

    /**
     * Tag used for logging from this fragment.
     */
    private static final String TAG = "EntrantAllEvents";

    /**
     * Argument key for passing an Entrant's hardware ID into this fragment.
     */
    public static final String ARG_ENTRANT_HARDWARE_ID = "arg_entrant_hardware_id";

    /** The event to display. */
    private Event event;

    /** The hardware ID of the entrant viewing this event. */
    private String entrantHardwareId;

    /**
     * Required empty public constructor for fragment instantiation.
     */
    public EntrantEventDetailsFragment() {}

    /**
     * Initializes the fragment and retrieves the Event argument if provided.
     * This method reads the serialized Event instance from the fragment's
     * arguments. If the argument is present and of the correct type, it is stored for
     * use when populating the UI.
     *
     * @param savedInstanceState the previously saved state, or null if none
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Object obj = getArguments().getSerializable(ARG_EVENT);
            if (obj instanceof Event) {
                event = (Event) obj;
            }
            entrantHardwareId = getArguments().getString(ARG_ENTRANT_HARDWARE_ID);
        }
    }

    /**
     * Inflates and populates the event details layout.
     * This method inflates the event details layout, binds view references, and fills
     * them with information from the current Event. If no event is available, an
     * unpopulated view is returned.
     *
     * @param inflater  the layout inflater used to inflate the fragment's view
     * @param container the parent view that the fragment's UI should be attached to, or null
     * @param savedInstanceState the previously saved state, or null if none
     * @return the root view for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        if (event == null) {
            return view;
        }

        // Find views
        ImageView imagePoster = view.findViewById(R.id.image_event_poster);
        TextView textEventName = view.findViewById(R.id.text_event_name);
        TextView textDescription = view.findViewById(R.id.text_event_description);
        TextView textLotteryCriteria = view.findViewById(R.id.text_lottery_criteria);
        TextView textStartTime = view.findViewById(R.id.text_start_time);
        TextView textLocation = view.findViewById(R.id.text_location);
        TextView textRegistrationWindow = view.findViewById(R.id.text_registration_window);
        TextView textGeolocationRequired = view.findViewById(R.id.text_geolocation_required);
        TextView textOrganizerId = view.findViewById(R.id.text_organizer_id);
        TextView textWaitlistInfo = view.findViewById(R.id.text_waitlist_info);
        TextView textRegistrationStatus = view.findViewById(R.id.text_registration_status);
        TextView textEntrantStatus = view.findViewById(R.id.text_entrant_status);

        // Date formatter
        DateFormat dateTimeFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        // Poster
        String posterUrl = event.getPosterURL();
        if (!TextUtils.isEmpty(posterUrl)) {
            imagePoster.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(posterUrl)
                    .into(imagePoster);
        } else {
            imagePoster.setVisibility(View.GONE);
        }

        // Basic fields
        textEventName.setText(event.getEventName());
        textDescription.setText(event.getEventDescription());

        // Lottery criteria
        if (!TextUtils.isEmpty(event.getLotteryCriteria())) {
            textLotteryCriteria.setVisibility(View.VISIBLE);
            textLotteryCriteria.setText("Lottery criteria: " + event.getLotteryCriteria());
        } else {
            textLotteryCriteria.setVisibility(View.GONE);
        }

        // Start time
        if (event.getStartTime() != null) {
            textStartTime.setText("Starts: " + dateTimeFormat.format(event.getStartTime()));
        } else {
            textStartTime.setText("Start time: Not set");
        }

        // Location
        if (!TextUtils.isEmpty(event.getLocation())) {
            textLocation.setText("Location: " + event.getLocation());
        } else {
            textLocation.setText("Location: Not set");
        }

        // Registration window
        StringBuilder regWindowBuilder = new StringBuilder("Registration: ");
        if (event.getRegistrationStartTime() != null) {
            regWindowBuilder.append(dateTimeFormat.format(event.getRegistrationStartTime()));
        } else {
            regWindowBuilder.append("N/A");
        }
        regWindowBuilder.append("  -  ");
        if (event.getRegistrationEndTime() != null) {
            regWindowBuilder.append(dateTimeFormat.format(event.getRegistrationEndTime()));
        } else {
            regWindowBuilder.append("N/A");
        }
        textRegistrationWindow.setText(regWindowBuilder.toString());

        // Registration status
        String status = event.getRegistrationStatus();
        if (!TextUtils.isEmpty(status)) {
            textRegistrationStatus.setVisibility(View.VISIBLE);
            textRegistrationStatus.setText(status);
        } else {
            textRegistrationStatus.setVisibility(View.GONE);
        }

        // Geolocation requirement
        textGeolocationRequired.setText(
                event.getRequiresGeolocation()
                        ? "Geolocation required for registration"
                        : "Geolocation not required"
        );

        // Organizer
        textOrganizerId.setText("Organizer ID: " + event.getEventOrganizerHardwareID());

        // Waitlist info (count only, no details)
        int waitlistSize = event.getWaitListEntrants() != null
                ? event.getWaitListEntrants().size()
                : 0;
        Integer waitlistLimit = event.getWaitlistLimit();

        String waitlistText;
        if (waitlistLimit != null) {
            waitlistText = "Waitlist: " + waitlistSize + " / " + waitlistLimit;
        } else {
            waitlistText = "Waitlist: " + waitlistSize + " (no limit)";
        }
        textWaitlistInfo.setText(waitlistText);

        // entrant's status
        if (!TextUtils.isEmpty(entrantHardwareId)) {
            Event.EntrantStatus entrantStatus = event.getEntrantStatus(entrantHardwareId);

            switch (entrantStatus) {
                case ACCEPTED:
                    textEntrantStatus.setText("Your status: Accepted");
                    textEntrantStatus.setVisibility(View.VISIBLE);
                    break;
                case INVITED:
                    textEntrantStatus.setText("Your status: Invited");
                    textEntrantStatus.setVisibility(View.VISIBLE);
                    break;
                case WAITLISTED:
                    textEntrantStatus.setText("Your status: On waitlist");
                    textEntrantStatus.setVisibility(View.VISIBLE);
                    break;
                case DECLINED:
                    textEntrantStatus.setText("Your status: Declined");
                    textEntrantStatus.setVisibility(View.VISIBLE);
                    break;
                case NONE:
                default:
                    textEntrantStatus.setVisibility(View.GONE);
                    break;
            }
        } else {
            textEntrantStatus.setVisibility(View.GONE);
        }

        return view;
    }

}