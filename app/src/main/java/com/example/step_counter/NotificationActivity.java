package com.example.step_counter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;

import java.sql.Time;
import java.util.Calendar;

public class NotificationActivity extends AppCompatActivity {
    private static final int TIME_NOTIFICATION_ID = 1;

    private final String DAILY_KEY = "daily";
    private final String WEATHER_ALARM_KEY = "weather_alarm";
    private final String LOCATION_ALARM_KEY = "location_alarm";

    private SwitchCompat dailyWalkSwitch;
    private SwitchCompat weatherWalkSwitch;
    private SwitchCompat locationWalkSwitch;

    //Shared Preferences
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.step_counter";

    //Alarm flags
    private boolean dailyAlarmFlag;
    private boolean weatherAlarmFlag;
    private boolean locationAlarmFlag;

    //alarm manager
    AlarmManager alarmManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        dailyWalkSwitch = findViewById(R.id.daily_switch);
        weatherWalkSwitch = findViewById(R.id.weather_switch);
        locationWalkSwitch = findViewById(R.id.location_switch);

        //load form shared prefs
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        dailyAlarmFlag = mPreferences.getBoolean(DAILY_KEY, false);
        Log.d("daily flag:",Boolean.toString(dailyAlarmFlag));
        weatherAlarmFlag = mPreferences.getBoolean(WEATHER_ALARM_KEY, false);
        locationAlarmFlag = mPreferences.getBoolean(LOCATION_ALARM_KEY, false);

        dailyWalkSwitch.setChecked(dailyAlarmFlag);
        weatherWalkSwitch.setChecked(weatherAlarmFlag);
        locationWalkSwitch.setChecked(locationAlarmFlag);

        //alarm manager
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //Set listeners for switches
        //Time-Triggered Notification
        dailyWalkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                if(checked){
                    Calendar c = Calendar.getInstance();//get time now
                    //Time-Triggered Notification
                    //Set alarm for time-triggered notification
                    //Send notification at 13.00 every day
                    Intent timeIntent = new Intent(NotificationActivity.this, TimeTriggeredReceiver.class);
                    PendingIntent timePendingIntent = PendingIntent.getBroadcast(NotificationActivity.this, TIME_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    c = Calendar.getInstance();//set time to now
                    c.set(Calendar.HOUR_OF_DAY, 13); //set time to 1.00 pm
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    Calendar now = Calendar.getInstance();

                    //Time for the alarm to go off has passed. Set alarm for tomorrow.
                    if(now.getTimeInMillis() > c.getTimeInMillis()){
                        c.add(Calendar.DATE,1);
                    }
                    Log.d("MA TT alarm", String.valueOf(c.getTime()));
                    //Need to be exact so use setInexactRepeating
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, timePendingIntent);

                }else{
                    Intent timeIntent = new Intent(NotificationActivity.this, TimeTriggeredReceiver.class);
                    PendingIntent timePendingIntent = PendingIntent.getBroadcast(NotificationActivity.this, TIME_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_NO_CREATE);
                    if(timePendingIntent != null) {
                        alarmManager.cancel(timePendingIntent);
                    }
                }
                preferencesEditor.putBoolean(DAILY_KEY, checked);
                preferencesEditor.apply();
            }
        });
        weatherWalkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                preferencesEditor.putBoolean(WEATHER_ALARM_KEY, checked);
                preferencesEditor.apply();
            }
        });
        locationWalkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                preferencesEditor.putBoolean(LOCATION_ALARM_KEY, checked);
                preferencesEditor.apply();
            }
        });

    }
}