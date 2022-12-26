package com.example.step_counter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.core.app.NotificationCompat;

//Broadcaster receiver for Timed-triggered notification
public class TimeTriggeredReceiver extends BroadcastReceiver {

    //Notification setup
    private NotificationManager mNotificationManager;
    private static final String TIME_CHANNEL_ID = "time-triggered_notification_channel";
    private static final int TIME_NOTIFICATION_ID = 1;



    @Override
    public void onReceive(Context context, Intent intent) {

        //The time has come, send notification
        Log.d("TT reciver", "fired");
        //get notification manager
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //send notification
        deliverNotification(context);

    }

    //Delivers the notification
    //Based on Android colab https://developer.android.com/codelabs/android-training-alarm-manager?index=..%2F..%2Fandroid-training#3
    private void deliverNotification(Context context) {

        Intent timeIntent = new Intent(context, MainActivity.class);
        PendingIntent timePendingIntent = PendingIntent.getActivity(context, TIME_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_big_icon);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TIME_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(bitmap)
                .setContentTitle(context.getString(R.string.time_notification_title))
                .setContentText(context.getString(R.string.time_notification_message))
                .setContentIntent(timePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        mNotificationManager.notify(TIME_NOTIFICATION_ID, builder.build());

    }
}