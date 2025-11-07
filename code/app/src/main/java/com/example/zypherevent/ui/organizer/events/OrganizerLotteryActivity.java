package com.example.zypherevent.ui.organizer.events;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**Author: Britney Kunchidi
 * Simple Activity to run the lottery for an event.
 * Uses popup_organizer_lottery.xml as its layout.
 * For now, uses a dummy waitlist just to demonstrate random sampling.
 *
 * TODO: replace dummy waitlist with real entrants from Database / Firestore.
 */
public class OrganizerLotteryActivity extends AppCompatActivity {

    private RecyclerView rvWaitlist;
    private EditText etSampleSize;
    private Button btnRunLottery;

    // TEMP: fake waitlist so we can test the sampling logic
    private List<String> waitlist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_organizer_lottery);

        // Find views from popup_organizer_lottery.xml
        rvWaitlist = findViewById(R.id.entrant_waitlist);
        etSampleSize = findViewById(R.id.etSampleSize);
        btnRunLottery = findViewById(R.id.run_lottery);

        // TODO later: set up RecyclerView adapter to show real entrants
        setupDummyWaitlist();

        btnRunLottery.setOnClickListener(v -> runLottery());
    }

    /**
     * TEMP: Create a dummy list of entrants so we can test the sampling.
     * Later this should be replaced by real entrants loaded from the Database.
     */
    private void setupDummyWaitlist() {
        // For now, pretend we have 10 entrants
        for (int i = 1; i <= 10; i++) {
            waitlist.add("Entrant " + i);
        }
        // TODO: connect a RecyclerView adapter here if you want to display them
    }

    /**
     * Reads the number to sample, randomly selects that many entrants from the waitlist,
     * and shows the result in a Toast.
     *
     * This satisfies:
     * - random selection
     * - equal chance of being chosen (we shuffle the whole list once)
     */
    private void runLottery() {
        String input = etSampleSize.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a number to sample", Toast.LENGTH_SHORT).show();
            return;
        }

        int sampleSize;
        try {
            sampleSize = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sampleSize <= 0) {
            Toast.makeText(this, "Sample size must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (waitlist.isEmpty()) {
            Toast.makeText(this, "No entrants in waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Don’t ask for more than we have
        int n = Math.min(sampleSize, waitlist.size());

        // Randomly shuffle the waitlist so everyone has an equal chance
        Collections.shuffle(waitlist);

        // Pick the first n entrants after shuffle
        List<String> selected = waitlist.subList(0, n);

        // For now, just show them in a Toast
        Toast.makeText(
                this,
                "Selected " + n + " entrants: " + selected,
                Toast.LENGTH_LONG
        ).show();

        //  trigger notifications (02.05.01)
    }
}
