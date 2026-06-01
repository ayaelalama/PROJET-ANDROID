package com.example.td2ex2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Notification pour le secouriste — redirige vers MainActivity
 * pour que le code d'accès soit toujours demandé. (Notion 12)
 */
public class SecoursNotificationHelper {

    private static final String CHANNEL_ID   = "secours_channel";
    private static final String CHANNEL_NAME = "Alertes Secours";
    private static int notifId = 1000;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Nouveaux signalements d'accidents");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    public static void notifyNouvelIncident(Context context, Issue issue) {
        createChannel(context);

        // Redirige vers MainActivity — le code secours sera toujours demandé
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_secours", true); // indique qu'on veut ouvrir secours

        PendingIntent pi = PendingIntent.getActivity(
                context, notifId, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String priorityLabel;
        switch (issue.getPriority()) {
            case CRITICAL: priorityLabel = "CRITIQUE"; break;
            case HIGH:     priorityLabel = "Élevée";   break;
            case MEDIUM:   priorityLabel = "Moyenne";  break;
            default:       priorityLabel = "Faible";   break;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Nouvel incident — " + priorityLabel)
                .setContentText(issue.getTitle())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Accident signalé\n"
                                + issue.getTitle() + "\n"
                                + "Gravité : " + priorityLabel + "\n"
                                + "GPS : " + String.format("%.4f, %.4f",
                                issue.getLatitude(), issue.getLongitude())
                                + "\nOuvrez l'interface secours pour intervenir."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setVibrate(new long[]{0, 300, 100, 300});

        try {
            NotificationManagerCompat.from(context).notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Permission POST_NOTIFICATIONS non accordée
        }
    }
}
