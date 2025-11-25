package com.example.zypherevent.ui.entrant.events;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.Utils;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

/**
 * QR scanner for entrants to discover events.
 */
public class EntrantQRScannerFragment extends Fragment {

    private static final String TAG = "EntrantQRScanner";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private DecoratedBarcodeView barcodeView;
    private Database db;
    private boolean isScanning = false; // Prevents multiple scans while processing

    public EntrantQRScannerFragment() {
        // public no-arg constructor required
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_qr_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new Database();
        barcodeView = view.findViewById(R.id.barcode_scanner);

        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeScanner();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Initializes the barcode scanner with a callback for handling scan results.
     */
    private void initializeScanner() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                // Only process if we have a result and aren't already processing one
                if (result != null && !isScanning) {
                    isScanning = true;
                    handleScanResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List resultPoints) {
                // Optional: handle possible result points for visual feedback
            }
        });
    }

    /**
     * After scan result, this function handles navigating to the events page
     *
     * @param qrContent the contents of the qr code scanning result
     */
    private void handleScanResult(String qrContent) {
        Log.d(TAG, "Scanned QR code: " + qrContent);

        Long eventId = Utils.extractEventId(qrContent);

        if (eventId != null) {
            Toast.makeText(getContext(), "Loading event...", Toast.LENGTH_SHORT).show();

            db.getEvent(eventId).addOnCompleteListener(task -> {
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment detached before event loaded");
                    return;
                }

                if (task.isSuccessful() && task.getResult() != null) {
                    Event event = task.getResult();

                    Bundle bundle = new Bundle();
                    bundle.putSerializable(EntrantEventDetailsFragment.ARG_EVENT, event);

                    Navigation.findNavController(requireView()).navigate(R.id.nav_entrant_event_details, bundle);

                } else {
                    Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                    requireView().postDelayed(() -> isScanning = false, 2000);
                }
            });

        } else {
            Toast.makeText(getContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
            requireView().postDelayed(() -> isScanning = false, 2000);
        }
    }

    /**
     * Requests camera permission from the user.
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeScanner();
            } else {
                Toast.makeText(getContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Called when fragment becomes visible. Resumes camera preview and resets
     * scanning flag.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null) {
            barcodeView.resume();
            isScanning = false;
        }
    }

    /**
     * Called when fragment is no longer visible. Seems wasteful to keep this
     * running.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }
}
