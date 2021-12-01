package com.google.firebase.messaging;

import static com.google.firebase.messaging.Constants.TAG;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * Encapsulates the information required to display a notification.
 */
public class DisplayNotificationInfo {

    private final NotificationCompat.Builder notificationBuilder;
    private final String tag;
    private final int id;

    DisplayNotificationInfo(NotificationCompat.Builder notificationBuilder, String tag,
                                   int id) {
        this.notificationBuilder = notificationBuilder;
        this.tag = tag;
        this.id = id;
    }

    /**
     * Gets the notification builder for this Notification.
     * @return Instance of {@link NotificationCompat.Builder}
     */
    public NotificationCompat.Builder getNotificationBuilder(){
        return notificationBuilder;
    }

    /**
     * Shows the notification in notification tray.
     * @param context Application Context
     */
    public void showNotification(Context context) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Showing notification");
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(tag, id, notificationBuilder.build());
    }
}
