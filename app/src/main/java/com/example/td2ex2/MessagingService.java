package com.example.td2ex2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";
    private static final String CHANNEL_ID = "alerte_accident_channel";

    /**
     * Appelé lors de la génération ou du renouvellement du token Firebase.
     * En production, on enverrait ce token au serveur backend.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nouveau token Firebase : " + token);
        // TODO (production) : envoyer le token au serveur backend
    }

    /**
     * Appelé à la réception d'un message Firebase.
     * Gère les deux types de payload : "notification" et "data".
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Alerte Accident";
        String body  = "Nouvelle alerte reçue";

        // Payload "Notification" (standard Firebase console)
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        // Payload "Data" (clés personnalisées) — prioritaire sur le payload notification
        if (!remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();

            // Support des clés françaises ("titre"/"corps") et anglaises ("title"/"body")
            if (data.containsKey("titre"))       title = data.get("titre");
            else if (data.containsKey("title"))  title = data.get("title");

            if (data.containsKey("corps"))       body = data.get("corps");
            else if (data.containsKey("body"))   body = data.get("body");
        }

        sendNotification(title, body);
    }

    // ── Construction de la notification ──────────────────────────────────────

    private void sendNotification(String title, String body) {
        // Création du canal (Android 8.0+)
        createNotificationChannel();

        // PendingIntent : ouvre MainActivity au clic sur la notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),   // requestCode unique
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Vérification permission POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée — notification ignorée.");
                return;
            }
        }

        // ID unique basé sur le temps → chaque notification s'empile (ne s'écrase pas)
        int notificationId = (int) System.currentTimeMillis();
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    /**
     * Crée le canal de notification (obligatoire depuis Android 8.0 / API 26).
     * L'appel est idempotent : si le canal existe déjà, il n'est pas recréé.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alerte Accident",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications de l'application Alerte Accident");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}