package com.example.step_counter;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;

//BroadcastReceiver for when the device reboots. Resets alarms and sets up notification channels.
public class BootReceiver extends BroadcastReceiver {


    //SharedPreferences Keys
    private final String DAILY_KEY = "daily";
    private final String WEATHER_ALARM_KEY = "weather_alarm";
    private final String LOCATION_ALARM_KEY = "location_alarm";

    //Notification IDs
    private static final String ACTION_BOOT_BROADCAST = BuildConfig.APPLICATION_ID + ".ACTION_BOOT_BROADCAST";
    private static final int MIDNIGHT_BROADCAST_ID = 0;

    private static final String TIME_CHANNEL_ID = "time-triggered_notification_channel";
    private static final int TIME_NOTIFICATION_ID = 1;
    private static final int FENCE_ID = 4;
    final private String FENCE_KEY = "awareness_fence_key";


    private static final String WEATHER_CHANNEL_ID = "weather-triggered_notification_channel";
    private static final int WEATHER_NOTIFICATION_ID = 2;

    private static final String LOCATION_CHANNEL_ID = "location-triggered_notification_channel";
    private static final int LOCATION_NOTIFICATION_ID = 3;

    @Override
    public void onReceive(Context context, Intent intent) {

        //If device rebooted, need to setup alarms again
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("boot receiver", "fired");
            Intent sendingIntent = new Intent();
            sendingIntent.setAction(ACTION_BOOT_BROADCAST);
            LocalBroadcastManager.getInstance(context).sendBroadcast(sendingIntent);


        SharedPreferences sp = context.getSharedPreferences("com.example.step_counter", Context.MODE_PRIVATE);
        boolean dailyWalkFlag = sp.getBoolean(DAILY_KEY,false);
        boolean weatherWalkFlag = sp.getBoolean(WEATHER_ALARM_KEY, false);
        boolean locationWalkFlag = sp.getBoolean(LOCATION_ALARM_KEY, false);


        /*
        Reset Alarms
         */

        //midnight alarm
        Intent alarmIntent = new Intent(context, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, MIDNIGHT_BROADCAST_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.add(Calendar.DATE, 1);
        Log.d("BR midnight alarm", String.valueOf(c.getTime()));
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);

        //Time-triggered notification alarm
        Intent timeIntent = new Intent(context, TimeTriggeredReceiver.class);
        PendingIntent timePendingIntent = PendingIntent.getBroadcast(context, TIME_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        c = Calendar.getInstance();//set to now
        c.set(Calendar.HOUR_OF_DAY, 13);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        //move alarm time to tomorrow, if time has passed for alarm
        Calendar now = Calendar.getInstance();
        if(now.getTimeInMillis() > c.getTimeInMillis()){
            c.add(Calendar.DATE,1);
        }
        Log.d("BR TT Alarms", String.valueOf(c.getTime()));
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, timePendingIntent);

        //Weather-triggered notification alarm
        Intent weatherIntent = new Intent(context, WeatherTriggeredReceiver.class);
        PendingIntent weatherPendingIntent = PendingIntent.getBroadcast(context, WEATHER_NOTIFICATION_ID, weatherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        c = Calendar.getInstance(); //set to now
        c.set(Calendar.HOUR_OF_DAY, 15);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        //move to tomorrow, if time has passed for alarm
            now = Calendar.getInstance();
            if(now.getTimeInMillis() > c.getTimeInMillis()){
                c.add(Calendar.DATE,1);
            }
        Log.d("BR WT alarm", String.valueOf(c.getTime()));
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, weatherPendingIntent);


        //Location-triggered notification
        //Awareness Location
        // Hard Coded Location
            // Rottenrow Gardens
            //Rottenrow Gardens, Glasgow, UK
            //55.861914102300354, -4.244193324799351
            double lat = 55.861914102300354;
            double lon = -4.244193324799351;


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //Awareness fence setup.
        AwarenessFence locationFenceEnter = LocationFence.entering(lat, lon, 35);
        AwarenessFence locationFenceExit = LocationFence.exiting(lat, lon, 35);
        AwarenessFence combinedFence = AwarenessFence.or(locationFenceEnter, locationFenceExit);

        Intent fenceIntent = new Intent(context, AwarenessFenceReceiver.class);
        PendingIntent fencePendingIntent = PendingIntent.getBroadcast(context, FENCE_ID, fenceIntent, PendingIntent.FLAG_UPDATE_CURRENT );

        //Listen for if the device enters or exits the fence
        Awareness.getFenceClient(context).updateFences(new FenceUpdateRequest.Builder()
                .addFence(FENCE_KEY, combinedFence, fencePendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("fence listener", "Fence registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("fence listener", "Fence not registered: " + e);
                    }
                });

        //Setup Notification channels
        NotificationManager mNotificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel timeNotificationChannel = new NotificationChannel(TIME_CHANNEL_ID, "Time-triggered notification", NotificationManager.IMPORTANCE_HIGH);
            timeNotificationChannel.enableLights(true);
            timeNotificationChannel.setLightColor(Color.BLUE);
            timeNotificationChannel.enableVibration(true);
            timeNotificationChannel.setDescription("Notifies at 1.00 PM every day to go for a walk");
            mNotificationManager.createNotificationChannel(timeNotificationChannel);


            NotificationChannel locationNotificationChannel = new NotificationChannel(LOCATION_CHANNEL_ID, "Location-triggered notification", NotificationManager.IMPORTANCE_HIGH);
            locationNotificationChannel.enableLights(true);
            locationNotificationChannel.setLightColor(Color.MAGENTA);
            locationNotificationChannel.enableVibration(true);
            locationNotificationChannel.setDescription("Notifies when the device enters/exits a Awareness Location Fence.");
            mNotificationManager.createNotificationChannel(locationNotificationChannel);

            NotificationChannel weatherNotificationChannel = new NotificationChannel(WEATHER_CHANNEL_ID, "Weather-triggered notification", NotificationManager.IMPORTANCE_HIGH);
            weatherNotificationChannel.enableLights(true);
            weatherNotificationChannel.setLightColor(Color.GREEN);
            weatherNotificationChannel.enableVibration(true);
            weatherNotificationChannel.setDescription("Notifies at 3.00 pm every day to go for a walk is weather is clear");
            mNotificationManager.createNotificationChannel(weatherNotificationChannel);

        }
    }
}}
