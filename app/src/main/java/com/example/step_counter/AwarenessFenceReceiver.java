package com.example.step_counter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.awareness.fence.FenceState;


//BroadcastReceiver for Location Notification
public class AwarenessFenceReceiver extends BroadcastReceiver {
    private NotificationManager mNotificationManager;
    final private String FENCE_KEY = "awareness_fence_key";
    private static final String LOCATION_CHANNEL_ID = "location-triggered_notification_channel";
    private static final int LOCATION_NOTIFICATION_ID = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        FenceState fenceState = FenceState.extract(intent);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        //Check the state of the fence, if device entered to exited fence, send notification
        if(TextUtils.equals(fenceState.getFenceKey(),FENCE_KEY)){
            String fenceStateStr;
            switch (fenceState.getCurrentState()){
                case FenceState.TRUE:
                    fenceStateStr = "true";
                    deliverNotification(context);
                    break;
                case FenceState.FALSE:
                    fenceStateStr = "false";
                    break;
                case FenceState.UNKNOWN:
                    fenceStateStr = "unknown";
                    break;
                default:
                    fenceStateStr = "unknown value";
        }
    }
}

    //Delivers the notification
    //Based on Android colab https://developer.android.com/codelabs/android-training-alarm-manager?index=..%2F..%2Fandroid-training#3
    private void deliverNotification(Context context) {

        Intent locationIntent = new Intent(context, MainActivity.class);
        PendingIntent timePendingIntent = PendingIntent.getActivity(context, LOCATION_NOTIFICATION_ID, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOCATION_CHANNEL_ID);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_big_icon);
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(bm)
                .setContentTitle(context.getString(R.string.location_notifiaction_string))
                .setContentText(context.getString(R.string.location_notification_message))
                .setContentIntent(timePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        mNotificationManager.notify(LOCATION_NOTIFICATION_ID, builder.build());

    }

}
