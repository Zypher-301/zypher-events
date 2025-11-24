package com.example.zypherevent.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.R;

/**
 * Helper class for creating and displaying Android level notifications.
 * Manages notification channels and provides methods to show notifications.
 * Channel Structure:
 * - Event Invitations (high): lottery selections, acceptance
 * - Event Update (default): waitlist status, general updates
 * - Event Rejections (default): declined/rejected notifications
 * - Service (low): foreground service notification
 *
 * @author Tom Yang
 * @version 1.0
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    // Notification channel IDs
    private static final String CHANNEL_ID_INVITATIONS = "event_invitations";
    private static final String CHANNEL_ID_UPDATES = "event_updates";
    private static final String CHANNEL_ID_REJECTIONS = "event_rejections";
    private static final String FOREGROUND_CHANNEL_ID = "notification_service_channel";

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    /**
     * Constructor that initializes the notification helper and creates notification channels
     *
     * @param context Application context
     */
    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(this.context);
        createNotificationChannels();
    }

    /**
     * Creates notification channels
     * Creates separate channels for invitations, updates, rejections, and service
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Event Invitations (HIGH priority)
            NotificationChannel invitationsChannel = new NotificationChannel(
                    CHANNEL_ID_INVITATIONS,
                    "Event Invitations",
                    NotificationManager.IMPORTANCE_HIGH
            );
            invitationsChannel.setDescription("Get notified when you're invited to events!");
            invitationsChannel.enableVibration(true);
            invitationsChannel.setShowBadge(true);

            // Channel 2: Event Updates (DEFAULT priority)
            NotificationChannel updatesChannel = new NotificationChannel(
                    CHANNEL_ID_UPDATES,
                    "Event Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            updatesChannel.setDescription("Stay updated on your event status");
            updatesChannel.setShowBadge(true);

            // Channel 3: Event Rejections (DEFAULT priority)
            NotificationChannel rejectionsChannel = new NotificationChannel(
                    CHANNEL_ID_REJECTIONS,
                    "Event Rejections",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            rejectionsChannel.setDescription("Notifications about declined invitations");
            rejectionsChannel.setShowBadge(false);

            // Channel 4: Notification Service (LOW priority)
            // For: Foreground service persistent notification
            NotificationChannel serviceChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Notification Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Keeps the app connected to receive event notifications");
            serviceChannel.setShowBadge(false);

            // Register all channels
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(invitationsChannel);
                manager.createNotificationChannel(updatesChannel);
                manager.createNotificationChannel(rejectionsChannel);
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    /**
     * Displays a HIGH priority notification for event invitations.
     * Use for: Lottery selections, acceptance confirmations
     *
     * @param notificationId Unique ID for this notification
     * @param title Notification title
     * @param message Notification message
     */
    public void showInvitationNotification(int notificationId, String title, String message) {
        showNotification(notificationId, title, message, CHANNEL_ID_INVITATIONS, true);
    }

    /**
     * Displays a DEFAULT priority notification for event updates.
     * Use for: Waitlist status, general event updates
     *
     * @param notificationId Unique ID for this notification
     * @param title Notification title
     * @param message Notification message
     */
    public void showUpdateNotification(int notificationId, String title, String message) {
        showNotification(notificationId, title, message, CHANNEL_ID_UPDATES, false);
    }

    /**
     * Displays a DEFAULT priority notification for event rejections.
     * Use for: Not selected, declined, rejected from event
     *
     * @param notificationId Unique ID for this notification
     * @param title Notification title
     * @param message Notification message
     */
    public void showRejectionNotification(int notificationId, String title, String message) {
        showNotification(notificationId, title, message, CHANNEL_ID_REJECTIONS, false);
    }

    /**
     * Core method for displaying a system level notification.
     * Creates a notification with the specified parameters and displays it.
     *
     * @param notificationId Unique ID for this notification
     * @param title Notification title
     * @param message Notification message
     * @param channelId The channel ID to use
     * @param highPriority Whether or not this notification is high priority
     */
    private void showNotification(int notificationId, String title, String message, String channelId, boolean highPriority) {
        try {
            Intent intent = new Intent(context, EntrantActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("navigate_to", "notifications"); // Navigates to notifications page

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notificationlog)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true); // This removes the notification when tapped on

            // Show the notification
            notificationManager.notify(notificationId, builder.build());

        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied to show notification", e);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    /**
     * Creates a notification for the foreground service
     * This is the persistent notification shown when app is running to listen for real time notifications
     *
     * @param title Notification title
     * @param message Notification message
     * @return Notification object for the foreground service
     */
    public android.app.Notification createForegroundNotification(String title, String message) {
        Intent notificationIntent = new Intent(context, EntrantActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notificationlog)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false);

        return builder.build();
    }
}
