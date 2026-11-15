package com.fp.breadfast.complaints;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** بيستقبل الإشعارات. لو التطبيق مقفول، أندرويد بيعرض الإشعار لوحده من غير ما يدخل هنا. */
public class PushService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        // الرقم الجديد بيتبعت للصفحة أول ما التطبيق يتفتح
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        String title = null, body = null;
        if (msg.getNotification() != null) {
            title = msg.getNotification().getTitle();
            body = msg.getNotification().getBody();
        }
        if (title == null) title = msg.getData().get("title");
        if (body == null) body = msg.getData().get("body");
        if (title == null) title = "Breadfast";
        if (body == null) body = "";

        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "complaints")
                : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_notify)
                .setColor(Color.parseColor("#A7077F"))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), b.build());
    }
}
