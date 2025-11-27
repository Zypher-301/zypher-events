package com.example.zypherevent.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.zypherevent.Database;
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This service has two main functions:
 * Sending notifications to users via Firestore
 * Listening for new notifications in real-time and displaying them as Android system notifications
 * <p>
 * The service runs in the foreground (persistent) to maintain the listener even
 * when the app is running in the background. Notifications are categorized based on their context taken
 * from the notification title or body.
 *
 * @author Tom Yang
 * @version 1.0
 * @see NotificationHelper
 * @see EntrantActivity
 */
public class NotificationService extends Service {
    private static final String TAG = "NotificationService";

    //  Foreground service notification
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final String FOREGROUND_CHANNEL_ID = "notification_service_channel";

    private final IBinder binder = new LocalBinder();
    private Database db;
    private NotificationHelper notificationHelper;
    private ListenerRegistration notificationListener;

    // Track notifications to avoid duplicates
    private final Set<Long> shownNotificationIds = new HashSet<>();

    private String currentUserHardwareId;
    private boolean isListening = false;

    /**
     * Binder class for local service binding
     */
    public class LocalBinder extends Binder {
        public NotificationService getService() {
            return NotificationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new Database();
        notificationHelper = new NotificationHelper(this);

        // Create the foreground notification channel
        createForegroundNotificationChannel();

        Log.d(TAG, "NotificationService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());
        Log.d(TAG, "NotificationService started as foreground service");

        // returns START_STICKY so the service attempts to restart if killed
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Creates the notification channel for the foreground service
     */
    private void createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID, "Notification Service", NotificationManager.IMPORTANCE_LOW); // low importance for no sound or alerts
            channel.setDescription("Keeps  the app connected to receive event notifications");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Creates the persistent notification shown while the service is running
     * This notification is required for foreground services.
     *
     * @return the foreground notification produced with the helper
     */
    private Notification createForegroundNotification() {
        return notificationHelper.createForegroundNotification(
                "Zypher Event",
                "Listening for event notification"
        );
    }

    /**
     * Updates the foreground notification with the current status
     *
     * @param status current status/state the listener is in
     */
    private void updateForegroundNotification(String status) {
        Intent notificationIntent = new Intent(this, EntrantActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Zypher Event")
                .setContentText(status)
                .setSmallIcon(R.drawable.ic_notificationlog)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(FOREGROUND_NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Sends notification to a specific user.
     * Creates the notification in Firestore
     *
     * @param senderHardwareId The hardware ID of the user sending the notification
     * @param receiverHardwareId The hardware ID of the user receiving the notification
     * @param title The notification title
     * @param message The notification message
     * @return Task that completes when teh notification is saved
     */
    public Task<Void> sendNotification(String senderHardwareId, String receiverHardwareId, String title, String message) {
        return db.getUniqueNotificationID().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            Long notificationId = task.getResult();
            com.example.zypherevent.Notification notification = new com.example.zypherevent.Notification(
                    notificationId,
                    senderHardwareId,
                    receiverHardwareId,
                    title,
                    message
            );

            return db.setNotificationData(notificationId, notification)
                    .addOnSuccessListener(v ->
                            Log.d(TAG, "Notification sent to: " + receiverHardwareId))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to send notification to: " + receiverHardwareId, e));
        });
    }

    /**
     * Sends notifications to multiple users
     * Used for bulk notifications like lottery results
     *
     * @param senderHardwareId The hardware ID of the user sending the notification
     * @param receiverIds The hardware ID of the user receiving the notification
     * @param title The notification title
     * @param message The notification message
     */
    public void sendBulkNotifications(String senderHardwareId, List<String> receiverIds, String title, String message) {
        for (String receiverId : receiverIds) {
            sendNotification(senderHardwareId, receiverId, title, message)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to send notification to: " + receiverId, e));
        }
    }

    /**
     * Starts listening for new notifications for a specific user.
     * When new notifications arrive, displays those as android system notifications.
     * Uses keywords from the notification itself to sort the notification priority
     *
     * @param userHardwareId The hardware ID of the user to listen for
     */
    public void startListeningForNotifications(String userHardwareId) {
        // stop existing listeners
        stopListeningForNotifications();

        this.currentUserHardwareId = userHardwareId;
        this.isListening = true;

        updateForegroundNotification("Connected - Listening for notifications");

        // Load existing notifications Ids to avoid showing old notifications
        loadExistingNotificationIds(userHardwareId);

        // Set up the real-time listener for new notifications
        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("receivingUserHardwareID", userHardwareId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listener failed", error);
                        updateForegroundNotification("Connection error - Retrying");
                        return;
                    }

                    if (querySnapshot != null) {
                        int newNotification = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                Long notificationId = doc.getLong("notificationID");

                                if (notificationId != null && !shownNotificationIds.contains(notificationId)) {
                                    String title = doc.getString("notificationHeader");
                                    String body = doc.getString("notificationBody");

                                    // Determine notification type based on content
                                    if (title != null && body != null) {
                                        String titleLower = title.toLowerCase();
                                        String bodyLower = body.toLowerCase();

                                        // Sort notification type based on keywords to set priority
                                        if (titleLower.contains("selected") ||
                                                titleLower.contains("invitation") ||
                                                titleLower.contains("invited") ||
                                                titleLower.contains("accepted") ||
                                                titleLower.contains("confirmed") ||
                                                bodyLower.contains("congratulations")) {
                                            // Set to HIGH priority
                                            notificationHelper.showInvitationNotification(notificationId.intValue(), title, body);

                                        } else if (titleLower.contains("rejected") ||
                                                titleLower.contains("declined") ||
                                                titleLower.contains("not selected") ||
                                                titleLower.contains("removed") ||
                                                bodyLower.contains("unfortunately") ||
                                                bodyLower.contains("not accepted")) {
                                            // Set to DEFAULT priority
                                            notificationHelper.showRejectionNotification(notificationId.intValue(), title, body);

                                        } else {
                                            // Set everything else to DEFAULT priority: general updates or waitlist status
                                            notificationHelper.showUpdateNotification(notificationId.intValue(), title, body);
                                        }

                                        shownNotificationIds.add(notificationId);
                                        newNotification++;
                                        Log.d(TAG,"Displayed new notification: " + title);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing notification", e);
                            }
                        }

                        if (newNotification > 0) {
                            updateForegroundNotification("Active - " + newNotification + " new notifications(s)");
                        }
                    }
                });
        Log.d(TAG, "Started listening for notifications for user: " + userHardwareId);
    }

    /**
     * Loads existing notification Ids to prevent showing olf notifications as new
     *
     * @param userHardwareId The user's hardware ID
     */
    private void loadExistingNotificationIds(String userHardwareId) {
        db.getAllNotifications()
                .addOnSuccessListener(notifications -> {
                    for (com.example.zypherevent.Notification notification : notifications) {
                        if (userHardwareId.equals(notification.getReceivingUserHardwareID())) {
                            shownNotificationIds.add(notification.getUniqueNotificationID());
                        }
                    }
                    Log.d(TAG, "Loaded " + shownNotificationIds.size() + " existing notification IDs");
                })
                .addOnFailureListener(e ->
                    Log.e(TAG, "Failed to load existing notifications", e));
    }

    /**
     * Stops listening for new notifications
     * Used for when the user does not want to receive notification
     */
    public void stopListeningForNotifications() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
            isListening = false;
            updateForegroundNotification("Disconnected");
            Log.d(TAG, "Stopped listening for notifications");
        }
    }

    /**
     * Clears cache of shown notification IDs
     * Used for when user delete account
     */
    public void clearNotificationCache() {
        shownNotificationIds.clear();
    }

    /**
     * Checks if the service is currently listening for notifications
     *
     * @return Boolean for isListening or not
     */
    public boolean isListening() {
        return isListening;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListeningForNotifications();
        Log.d(TAG, "NotificationService destroyed");
    }
}
