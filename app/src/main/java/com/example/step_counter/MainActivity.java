package com.example.step_counter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import uk.ac.ox.eng.stepcounter.StepCounter;



public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSIONS_REQUEST_OLDER = 1;
    private static final int PERMISSIONS_REQUEST_NEWER = 2;
    private static final int PERMISSIONS_REQUEST_NEWER_BACKGROUND = 3;

    //Shared Preferences
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.step_counter";

    //Current Date and steps
    private String mCurrentDate;
    private int mCurrentSteps = -1;

    //Shared Player Preferences Keys
    private final String DATE_KEY = "date";
    private final String STEPS_KEY = "steps";

    //Midnight Reset Receiver
    private BroadcastReceiver midnightReceiver;

    //Utility Broadcast Actions and IDs
    private static final String ACTION_BOOT_BROADCAST = BuildConfig.APPLICATION_ID + ".ACTION_BOOT_BROADCAST";
    private static final String ACTION_MIDNIGHT_BROADCAST = BuildConfig.APPLICATION_ID + ".ACTION_MIDNIGHT_BROADCAST";
    private static final int MIDNIGHT_BROADCAST_ID = 0;
    private static final int MIDNIGHT_NOTIFIER_ID = 0;


    //Broadcast IDs for Awareness Fence
    final private String FENCE_KEY = "awareness_fence_key";
    private static final int FENCE_ID = 4;

    //Notification IDs
    private NotificationManager mNotificationManager;
    private static final String TIME_CHANNEL_ID = "time-triggered_notification_channel";
    private static final int TIME_NOTIFICATION_ID = 1;

    private static final String WEATHER_CHANNEL_ID = "weather-triggered_notification_channel";
    private static final int WEATHER_NOTIFICATION_ID = 2;

    private static final String LOCATION_CHANNEL_ID = "location-triggered_notification_channel";
    private static final int LOCATION_NOTIFICATION_ID = 3;


    //Fields for Views
    Button mHistoryButton;
    Button mAddButton;
    HistoryViewModel mHistoryViewModel;

    //Fields for pedometer
    private SensorManager mSensorManager;
    private Sensor mSensorPedometer;
    private TextView mStepsText;


    //Fields for Oxford Step Counter
    private StepCounter oxfordCounter;
    private Sensor mSensorAccelerometer;
    private int lastSteps =0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get current date
        mCurrentDate = DateHelper.getCurrentDate();
        //Setup Steps UI
        mStepsText = findViewById(R.id.stepsText);



        //Setup Service Managers
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //Get the steps from the Shared Preferences and update steps UI
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        mCurrentSteps = mPreferences.getInt(STEPS_KEY, 0);
        mStepsText.setText(Integer.toString(mCurrentSteps));

        //Get the Step Detector sensor for device, if it exists. Else, get accelerometer
        mSensorPedometer = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (mSensorPedometer == null) {
            mSensorAccelerometer =mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Toast.makeText(this,"No Step Sensor Detected. Using Oxford Sensor",Toast.LENGTH_LONG).show();

            //Setup Oxford step counter.
            //https://github.com/Oxford-step-counter
            oxfordCounter = new StepCounter(100);
            oxfordCounter.addOnStepUpdateListener(new StepCounter.OnStepUpdateListener() {
                //This works fine in the emulator but not with my phones.
                @Override
                public void onStepUpdate(int steps) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Add steps
                            mCurrentSteps = mCurrentSteps + (steps-lastSteps);
                            lastSteps = steps;

                            //have to do it this way for some reason.
                            String str = Integer.toString(mCurrentSteps);
                            mStepsText.setText(str);
                        }
                    });

                }
            });
        }

        //Get History database ViewModel
        mHistoryViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);


        //Setup midnight reset Alarm
        Intent alarmIntent = new Intent(this, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, MIDNIGHT_BROADCAST_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar c = Calendar.getInstance();//get time now
        //set clock to 00.00
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        //Midnight is always the next day, so add a day
        c.add(Calendar.DATE, 1);
        Log.d("MA mid alarm",String.valueOf(c.getTime()));
        //Need to have this go off at midnight, so using setExactAndAllowWhileIdle
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);



        //Time-Triggered Notification
        //Set alarm for time-triggered notification
        //Send notification at 13.00 every day
        Intent timeIntent = new Intent(this, TimeTriggeredReceiver.class);
        PendingIntent timePendingIntent = PendingIntent.getBroadcast(this, TIME_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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

        //Permission checks
        //based on https://developer.android.com/training/permissions/requesting
        //https://stackoverflow.com/questions/41449652/activitycompat-requestpermissions
        //https://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_REQUEST_NEWER);

            //Can't ask for permission for Background Location. On my phone it just closes the app when I included it in the String array.
            //So, force app to close.
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //Based on https://stackoverflow.com/questions/26449985/how-to-close-an-app-after-showing-a-dialog
                        new AlertDialog.Builder(this)
                                .setTitle("Permission Required")
                                .setMessage("Location Permission for 'All the time' is needed for app to work. Please allow this in the app permission settings.")
                                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }}).show();
            }
        }else{
            //Get permissions for older Android OS
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_OLDER);
        }



        //Location-Triggered Notification
        //Send notification if device enters or exits a location Awareness fence
        //Rottenrow Gardens
        //Rottenrow Gardens, Glasgow, UK
        //55.861914102300354, -4.244193324799351
        double loc1_lat = 55.861914102300354;
        double loc1_lon = -4.244193324799351;



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        Log.d("key",BuildConfig.FENCE_API_KEY);
        //Awareness Fence setup. If device enters or exits the fence, trigger.
        AwarenessFence locationFenceEnter = LocationFence.entering(loc1_lat, loc1_lon, 60);
        AwarenessFence locationFenceExit = LocationFence.exiting(loc1_lat, loc1_lon, 60);
        AwarenessFence combinedFence = AwarenessFence.or(locationFenceEnter, locationFenceExit);
        Intent fenceIntent = new Intent(this, AwarenessFenceReceiver.class);
        PendingIntent fencePendingIntent = PendingIntent.getBroadcast(this, FENCE_ID, fenceIntent, PendingIntent.FLAG_UPDATE_CURRENT );

        //Setup awareness fence listener
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(FENCE_KEY, combinedFence, fencePendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("fence listener", "Fence was successfully registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("fence listener", "Fence could not be registered: " + e);
                    }
                });


        //Setup for transition to History activity
        mHistoryButton = findViewById(R.id.history_button);
        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), HistoryActivity.class);
                startActivity(intent);
            }
        });


        //Midnight broadcast receiver setup. If the MainActivity is open when midnight hits, need save current steps and reset the steps UI.
        //So, setup midnight BroadcastReceiver to responds to Alarm
        midnightReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //clear steps UI
                mStepsText.setText("0");

                //write current steps to database
                HistoryRoomDatabase db =  HistoryRoomDatabase.getDatabase(context);
                UpdateDB task = new UpdateDB(context, DateHelper.getYesterday(), mCurrentSteps);
                task.execute();

                //clear date/steps data
                mCurrentDate = DateHelper.getCurrentDate();
                mCurrentSteps = 0;
            }
        };


        //Weather Notification
        //Set alarm for weather-triggered notification
        //Only send notification if the weather is clear
        Intent weatherIntent = new Intent(this, WeatherTriggeredReceiver.class);
        PendingIntent weatherPendingIntent = PendingIntent.getBroadcast(this, WEATHER_NOTIFICATION_ID, weatherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        c = Calendar.getInstance();//set time to now
        c.set(Calendar.HOUR_OF_DAY, 15);//set time to 3.00 pm
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        //Time has passed for alarm to go off. Move it to tomorrow
        if(now.getTimeInMillis() > c.getTimeInMillis()){
            c.add(Calendar.DATE, 1);
        }
        Log.d("MA WT alarm",String.valueOf(c.getTime()));
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, weatherPendingIntent);

        //Set to receive midnight broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(midnightReceiver,new IntentFilter(ACTION_MIDNIGHT_BROADCAST));

        //Notifications setup
        createNotificationChannels();



        //Setup Worker to download weather data for Weather Notification when internet is not available.
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest downloadWorker = new PeriodicWorkRequest.Builder(WeatherDownloadWorker.class, 1, TimeUnit.DAYS)
                .setInputData(new Data.Builder().putString("weather_url","https://api.openweathermap.org/data/2.5/onecall?lat=35.70561972484302&lon=139.79219317117662&exclude=current,minutely,hourly,alerts&appid="+BuildConfig.OPEN_WEATHER_KEY).build())
                .setConstraints(constraints)
                .build();

      WorkManager.getInstance(this).enqueueUniquePeriodicWork("weatherDownloader", ExistingPeriodicWorkPolicy.KEEP,downloadWorker);




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //Set up menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        //Clear history, if selected from menu
        if(id == R.id.clear_history){
            Toast.makeText(this, "Clearing Step History...", Toast.LENGTH_LONG).show();
            mHistoryViewModel.deleteAllHistory();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart(){
        super.onStart();
        //Register step detector sensor
        if(mSensorPedometer != null) {
            mSensorManager.registerListener(this, mSensorPedometer, SensorManager.SENSOR_DELAY_FASTEST);
        }else if(mSensorAccelerometer != null){
            int periodusecs = (int) (1E6 / 100);
            mSensorManager.registerListener(accelerometerEventListener,mSensorAccelerometer,periodusecs);
        }
        //If using Oxford step counter, start it
        if(oxfordCounter != null){
            oxfordCounter.start();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        //if app paused save data
        this.mHistoryViewModel.doesDateExist(mCurrentDate).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    mHistoryViewModel.update(new Date(mCurrentDate, mCurrentSteps));
                }else{
                    mHistoryViewModel.insert(new Date(mCurrentDate, mCurrentSteps));
                }
            }
        });
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.putString(DATE_KEY, mCurrentDate);
        preferencesEditor.putInt(STEPS_KEY, mCurrentSteps);
        preferencesEditor.apply();
    }



    @Override
    protected void onResume(){
        super.onResume();
        //Mostly if returning from History after midnight
        mStepsText.setText(Integer.toString(mCurrentSteps));
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        //Write today's steps to database
        this.mHistoryViewModel.doesDateExist(mCurrentDate).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    mHistoryViewModel.update(new Date(mCurrentDate, mCurrentSteps));
                }else{
                    mHistoryViewModel.insert(new Date(mCurrentDate, mCurrentSteps));
                }
            }
        });

        //Save stuff
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.putString(DATE_KEY, mCurrentDate);
        preferencesEditor.putInt(STEPS_KEY, mCurrentSteps);
        preferencesEditor.apply();

        //Unregister step counter sensor
        mSensorManager.unregisterListener(this);
        mSensorManager.unregisterListener(accelerometerEventListener);
        //Unregister midnight broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(midnightReceiver);
        //stop Oxford step counter
        if(oxfordCounter != null){
            oxfordCounter.stop();
        }

        //Unregister Awareness Fence Listener
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder().removeFence(FENCE_KEY).build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("Unreg FenceListen", "Fence was unregistered");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Unreg FenceListen", "Fence not unregistered: " + e);
                    }
                });

    }


    //Sensor response
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Step was detected
        Log.d("onSensor mCurrent", Integer.toString(mCurrentSteps));

        //Get the type of sensor
        int sensorType = sensorEvent.sensor.getType();

        if(sensorType == Sensor.TYPE_STEP_DETECTOR) {
            //Get the step from the sensor. 1 step.
            float stepDetected = sensorEvent.values[0];
            //Add step to daily total. 1 step per detection
            mCurrentSteps += Math.round(stepDetected);
            //Update current steps on screen
            mStepsText.setText(Integer.toString(mCurrentSteps));
        //}else if(sensorType == Sensor.TYPE_ACCELEROMETER){
            //Oxford stuff
            //
            // oxfordCounter.processSample(sensorEvent.timestamp, sensorEvent.values);
        }else{
                //shouldn't get here, it was some other type of sensor
                Toast.makeText(this,"Something bad happened :(",Toast.LENGTH_LONG).show();
        }

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //do nothing, we don't use this
    }


    //Accelerometer listener for Oxford sensor.
    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            oxfordCounter.processSample(event.timestamp, event.values);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //do nothing, we don't use this
        }
    };


    /*
    Create Notification Channels
     */
    public void createNotificationChannels(){
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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

    /*
    Async class for updating the database
     */
    private static class UpdateDB extends AsyncTask {
        Context c;
        String day;
        int stepsForTheDay;

        public UpdateDB(Context c, String day, int steps){
            this.c = c;
            this.day =day;
            this.stepsForTheDay = steps;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            HistoryRoomDatabase db =  HistoryRoomDatabase.getDatabase(c);
            //If the date exists in the database update the steps, else, insert it as a new row
            if(db.historyDao().doesDateExist(day)){
                db.historyDao().update(new Date(day, stepsForTheDay));
            }else{
                db.historyDao().insert(new Date(day,stepsForTheDay));
            }
            return null;
        }

    }

    //Based on https://stackoverflow.com/questions/26449985/how-to-close-an-app-after-showing-a-dialog
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result: grantResults) {
            if(result == PackageManager.PERMISSION_DENIED){
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("Location Permission for 'All the time' and Activity permission needed. Please allow this in the app permission settings.")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }}).show();
            }
        }
    }
    }



