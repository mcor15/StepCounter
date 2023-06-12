package com.example.step_counter;

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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.exmaple.BagileviStepCounter.StepDetector;
import com.exmaple.BagileviStepCounter.StepListener;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {
    /*
    -Move alarms to Alarm Activity
    */
    private static final int PERMISSIONS_REQUEST_OLDER = 1;
    private static final int PERMISSIONS_REQUEST_NEWER = 2;
    private static final int PERMISSIONS_REQUEST_NEWER_BACKGROUND = 3;
    private static final int COUNTER_PERMISSIONS_REQUEST = 4;
    private static final int BACKGROUND_PERMISSIONS_REQUEST = 5;


    //Shared Preferences
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.step_counter";


    //Current Date and steps
    private String mCurrentDate;
    private int mCurrentSteps = -1;

    // Counter state
    private boolean counterState;

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
    SwitchCompat mCounterSwitch;

    //Fields for pedometer
    private SensorManager mSensorManager;
    private Sensor mSensorPedometer;
    private TextView mStepsText;


    //Fields for Bagievi Step Counter
    private Sensor mSensorAccelerometer;
    private int lastSteps =0;

    private StepDetector stepDetector;


    // Broadcast receiver for screen going blank
    BroadcastReceiver screenReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("state","on start");
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

        //Get History database ViewModel
        mHistoryViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // Screen Receiver
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                    mStepsText.setText(Integer.toString(mCurrentSteps));
                }

            }
        };
        IntentFilter screenIntent = new IntentFilter(Intent.ACTION_SCREEN_ON);
        this.registerReceiver(screenReceiver, screenIntent);

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
       // alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, timePendingIntent);



        //Location-Triggered Notification
        //Send notification if device enters or exits a location Awareness fence
        //Rottenrow Gardens
        //Rottenrow Gardens, Glasgow, UK
        //55.861914102300354, -4.244193324799351
        double loc1_lat = 55.861914102300354;
        double loc1_lon = -4.244193324799351;



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Awareness Fence setup. If device enters or exits the fence, trigger.
            AwarenessFence locationFenceEnter = LocationFence.entering(loc1_lat, loc1_lon, 60);
            AwarenessFence locationFenceExit = LocationFence.exiting(loc1_lat, loc1_lon, 60);
            AwarenessFence combinedFence = AwarenessFence.or(locationFenceEnter, locationFenceExit);
            Intent fenceIntent = new Intent(this, AwarenessFenceReceiver.class);
            PendingIntent fencePendingIntent = PendingIntent.getBroadcast(this, FENCE_ID, fenceIntent, PendingIntent.FLAG_UPDATE_CURRENT );

            //Setup awareness fence listener
            /*Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
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
                    });*/
        }else{
            //TODO
        }



        mCounterSwitch = findViewById(R.id.counter_switch);
        mCounterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Counter is off, turn it on
                if (isChecked){
                    Log.d("CounterToggle","ON");

                    //restore steps?
                    //Get the steps from the Shared Preferences and update steps UI
                    mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
                    mCurrentSteps = mPreferences.getInt(STEPS_KEY, 0);
                    Log.d("L_STEPS", Integer.toString(mCurrentSteps));
                    mStepsText.setText(Integer.toString(mCurrentSteps));

                    //request permission
                    //create list of permissions to check
                    String[] permins;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {//for android 10 and higher
                          permins = new String[]{  Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION};
                    }else{
                        permins = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION};
                    }

                    //still needs to request all permissions
                    boolean all_permissions_obtained = true;
                    for (String permission : permins) {
                        //if permission isn't granted, request it
                        //if user says ok, start counter in onRequestPermissionsResult()
                        if (!(ContextCompat.checkSelfPermission(compoundButton.getContext(), permission) == PackageManager.PERMISSION_GRANTED)) {
                            Log.d("Permin","request");
                            all_permissions_obtained = false;
                            ActivityCompat.requestPermissions(MainActivity.this, permins, COUNTER_PERMISSIONS_REQUEST);
                            break;
                        }

                    }
                    //We have all permissions to start the counter. So, start(register) the step-counter. Else do nothing since we will handle it in onRequestPermissionsResult()
                    if(all_permissions_obtained){
                        startStepCounter();
                    }
                }else{//Counter is running, turn it off

                    Log.d("CounterToggle","Stop Toggle");

                    //stop the counter and save steps

                    //Unregister step counter sensor
                    mSensorManager.unregisterListener(MainActivity.this);
                    //mSensorManager.unregisterListener(accelerometerEventListener);

                    //Write today's steps to database
                    MainActivity.this.mHistoryViewModel.doesDateExist(mCurrentDate).observe(MainActivity.this, new Observer<Boolean>() {
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
                    Log.d("STEPS", Integer.toString(mCurrentSteps));
                    preferencesEditor.apply();


                }
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

        //Setup for transition to Notifications activity
        mHistoryButton = findViewById(R.id.notification_button);
        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), NotificationActivity.class);
                startActivity(intent);
            }
        });


        //Midnight broadcast receiver setup. If the MainActivity is open when midnight hits, need save current steps and reset the steps UI.
        //So, setup midnight BroadcastReceiver to responds to Alarm
        midnightReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //clear steps UI element
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
        //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, weatherPendingIntent);

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

        /*
        if(mSensorPedometer != null) {
            mSensorManager.registerListener(this, mSensorPedometer, SensorManager.SENSOR_DELAY_FASTEST);
        }else if(mSensorAccelerometer != null){
            int periodusecs = (int) (1E6 / 100);
            mSensorManager.registerListener(accelerometerEventListener,mSensorAccelerometer,periodusecs);
        }
        //If using Oxford step counter, start it
        if(oxfordCounter != null){
            oxfordCounter.start();
        }*/
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
        Log.d("state","on resume");
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

        // Write today's steps to database
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

    // Unregister screen receiver
    this.unregisterReceiver(screenReceiver);

    }


    //Sensor response
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        //Get the type of sensor
        int sensorType = sensorEvent.sensor.getType();

        if(sensorType == Sensor.TYPE_STEP_DETECTOR) {
            //Step was detected
            Log.d("onSensorS mCurrent", Integer.toString(mCurrentSteps));
            //Get the step from the sensor. 1 step.
            float stepDetected = sensorEvent.values[0];
            //Add step to daily total. 1 step per detection
            mCurrentSteps += Math.round(stepDetected);
            //Update current steps on screen
            mStepsText.setText(Integer.toString(mCurrentSteps));
        }else if(sensorType == Sensor.TYPE_ACCELEROMETER){
            //Step was detected
            Log.d("onSensorA mCurrent", Integer.toString(mCurrentSteps));
            Log.d("A", Float.toString(sensorEvent.values[0])+" "+Float.toString(sensorEvent.values[1])+" "+Float.toString(sensorEvent.values[2]));
            stepDetector.updateAccel(
                    sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);

        }else{
                //shouldn't get here, it was some other type of sensor
                Toast.makeText(this,"Something bad happened :(",Toast.LENGTH_LONG).show();
        }

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //do nothing, we don't use this
    }


    //Accelerometer listener for Bagilevi step sensor.
    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //do nothing, we don't use this
        }
    };


    /**
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

    @Override
    public void step(long timeNs) {
        mCurrentSteps++;
        mStepsText.setText(Integer.toString(mCurrentSteps));

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

    /**
     * Stops the stepcounter and saves the steps
     */
    private void stopStepCounter(){
        //stop the counter and save steps

        //Unregister step counter sensor
        mSensorManager.unregisterListener(MainActivity.this);
        //mSensorManager.unregisterListener(accelerometerEventListener);


        //Write today's steps to database
        MainActivity.this.mHistoryViewModel.doesDateExist(mCurrentDate).observe(MainActivity.this, new Observer<Boolean>() {
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
        Log.d("STEPS", Integer.toString(mCurrentSteps));
        preferencesEditor.apply();
    }

    /**
     * Starts the stepcounter
     */
    private void startStepCounter(){
        if (mSensorPedometer == null) {
            Log.d("Sensor","no pedometer");
            lastSteps = 0;
            mSensorAccelerometer =mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Toast.makeText(MainActivity.this,"No Step Sensor Detected. Using Bagievi Sensor",Toast.LENGTH_LONG).show();



            //Setup Bagievi step counter.
            stepDetector = new StepDetector();
            stepDetector.registerListener(MainActivity.this);
        }
        //Register step detector sensor
        if(mSensorPedometer != null) {
            Log.d("stepsensor","yes");
            mSensorManager.registerListener(MainActivity.this, mSensorPedometer, SensorManager.SENSOR_DELAY_FASTEST);
        }else if(mSensorAccelerometer != null){
            mSensorManager.registerListener(MainActivity.this,mSensorAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == COUNTER_PERMISSIONS_REQUEST){
            boolean all_permissions_granted = true;
            for (int result: grantResults) {
                Log.d("permin","Fine location permissions");
                if(result == PackageManager.PERMISSION_DENIED){
                    all_permissions_granted = false;
                    break;
                }
        }
            if(all_permissions_granted){

                //request background (physical activity) if android 10 and above
                String[] permins;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    permins = new String[]{ Manifest.permission.ACCESS_BACKGROUND_LOCATION};
                    //If we don't have background permission, request it.
                    if (!(ContextCompat.checkSelfPermission(this, Arrays.toString(permins)) == PackageManager.PERMISSION_GRANTED)){
                        ActivityCompat.requestPermissions(MainActivity.this, permins, BACKGROUND_PERMISSIONS_REQUEST);
                    }
                }else{
                    //start counter
                    startStepCounter();
                }

            }else { //Permission denied for Fine Location or ACTIVITY_RECOGNITION
                //Based on https://stackoverflow.com/questions/26449985/how-to-close-an-app-after-showing-a-dialog
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("Location Permission for 'All the time' and Activity permission needed. Please allow this in the app permission settings.")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }}).show();
                mCounterSwitch.setChecked(false);

            }
     }else if(requestCode == BACKGROUND_PERMISSIONS_REQUEST){
            Log.d("permin","Background permissions"+Integer.toString(grantResults.length));
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                startStepCounter();
            }else if(grantResults.length>0){//Permission denied for Background permission
                //Based on https://stackoverflow.com/questions/26449985/how-to-close-an-app-after-showing-a-dialog
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("Location Permission for 'All the time' and Activity permission needed. Please allow this in the app permission settings.")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }}).show();
                mCounterSwitch.setChecked(false);
            }
        }
    }



    }




