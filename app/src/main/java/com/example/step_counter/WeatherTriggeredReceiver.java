package com.example.step_counter;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.*;

//Weather Broadcast Receiver for weather notification
public class WeatherTriggeredReceiver extends BroadcastReceiver {
    private NotificationManager mNotificationManager;

    private static final String WEATHER_CHANNEL_ID = "weather-triggered_notification_channel";
    private static final int WEATHER_NOTIFICATION_ID = 2;

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("weather-triggered", "fired");

        //get Notification manager
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d("weather-triggered", "mNote");


        //get device location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Log.d("weather-triggered", "fused client");


        //Request template
        //https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={API key}


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //Based on https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        //do we have internet access?
        if(networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Log.d("weather-triggered", "network new android");

            //Newer Android OS? Use FusedLocationClient
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(context.getMainExecutor(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();

                            //create request uri
                            String locationString = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid="+ BuildConfig.OPEN_WEATHER_KEY;
                            Log.d("cLocation", location.toString());


                            //Based on https://stackoverflow.com/questions/12575068/how-to-get-the-result-of-onpostexecute-to-main-activity-because-asynctask-is-a
                            //https://stackoverflow.com/questions/33229869/get-json-data-from-url-using-android
                            //http://theoryapp.com/parse-json-in-java/
                            //Parse the returned JSON weather.
                                JSONAsyncTask asyncTask = (JSONAsyncTask) new JSONAsyncTask(new JSONAsyncTask.JSONAsyncResponse() {
                                    @Override
                                    public void processFinish(String output) {
                                        Log.d("processfin", output);
                                        String jsonString = output.toString();
                                        JSONObject jObj = null;
                                        try {
                                            jObj = new JSONObject(jsonString);
                                            Log.d("job", jsonString.toString());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            if (jObj == null) {

                                            } else {
                                                //Check weather condition of 'main'. Can be:Thunderstorm, Drizzle, Rain, Snow, Clear, Clouds,
                                                // Mist, Smoke, Haze, Dust, Fog, Sand, Dust, Ash , Squall, Tornado.
                                                JSONArray jArray = jObj.getJSONArray("weather");
                                                JSONObject sub = new JSONObject(String.valueOf(jArray.getJSONObject(0)));
                                                Log.d("condition", sub.getString("main"));
                                                //If the weather is "Clear", send notification
                                                if (sub.getString("main").equals("Clear")) {
                                                    deliverNotification(context);
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).execute(locationString);

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("fail","fsued location");
                    }
                });
            }else{
                //can't use fusedLocationClient. device is older android
                Log.d("weather-triggered", "fused older android");
                //based on https://stackoverflow.com/questions/16498450/how-to-get-android-gps-location
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                Criteria crit = new Criteria();
                crit.setAccuracy(Criteria.ACCURACY_FINE);
                String best = locationManager.getBestProvider(crit, false);
                Location loc = locationManager.getLastKnownLocation(best);
                double lat = loc.getLatitude();
                double lon = loc.getLongitude();

                //Setup uri string
                String locationString = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid="+BuildConfig.OPEN_WEATHER_KEY;


                JSONAsyncTask asyncTask = (JSONAsyncTask) new JSONAsyncTask(new JSONAsyncTask.JSONAsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        Log.d("processfin", output);
                        String jsonString = output.toString();
                        JSONObject jObj = null;
                        try {
                            jObj = new JSONObject(jsonString);
                            Log.d("job", jsonString.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        try {
                            if (jObj == null) {

                            } else {
                                //Check weather condition of 'main'. Can be:Thunderstorm, Drizzle, Rain, Snow, Clear, Clouds,
                                // Mist, Smoke, Haze, Dust, Fog, Sand, Dust, Ash , Squall, Tornado.
                                JSONArray jArray = jObj.getJSONArray("weather");
                                JSONObject sub = new JSONObject(String.valueOf(jArray.getJSONObject(0)));
                                Log.d("condition", sub.getString("main"));
                                //If the weather is "Clear", send notification
                                if (sub.getString("main").equals("Clear")) {
                                    deliverNotification(context);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).execute(locationString);

            }
        }else{
            //No internet, use weather data from db
            CheckWeather weatherTask = new CheckWeather(context);
            weatherTask.execute();
            Log.d("weather-triggered", "no internet");
        }



    }


    //Based on https://stackoverflow.com/questions/12575068/how-to-get-the-result-of-onpostexecute-to-main-activity-because-asynctask-is-a
    //And https://stackoverflow.com/questions/33229869/get-json-data-from-url-using-android
    //Send request to OpenWeatherMap Api and retrieve a JSON.
    public static class JSONAsyncTask extends AsyncTask<String, Void, String> {
        HttpURLConnection connection = null;
        BufferedReader reader = null;


        public interface JSONAsyncResponse {
            void processFinish(String output);
        }

        public JSONAsyncResponse delegate = null;

        public JSONAsyncTask(JSONAsyncResponse delegate){
            this.delegate = delegate;
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d("doin", strings[0]);
            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("onPost", result);
            delegate.processFinish(result);
        }
    }

    //Delivers the notification
    //Based on Android colab https://developer.android.com/codelabs/android-training-alarm-manager?index=..%2F..%2Fandroid-training#3
    private void deliverNotification(Context context) {
        Intent timeIntent = new Intent(context, MainActivity.class);
        PendingIntent timePendingIntent = PendingIntent.getActivity(context, WEATHER_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WEATHER_CHANNEL_ID);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_big_icon);
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(bm)
                .setContentTitle(context.getString(R.string.weather_notification_title))
                .setContentText(context.getString(R.string.weather_notfication_message))
                .setContentIntent(timePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        mNotificationManager.notify(WEATHER_NOTIFICATION_ID, builder.build());

    }

    //Check weather stored in db to see if it's clear.
    public static class CheckWeather extends AsyncTask{
        private Context context;

        CheckWeather(Context c){
            this.context = c;
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            //Get weather db
            WeatherRoomDatabase db = WeatherRoomDatabase.getDatabase(context);
            Weather[] weatherData = db.weatherDao().getWeather();

            if (db != null){
                String today = DateHelper.getCurrentDate();

                for(int x =0 ; x<weatherData.length;x++){
                    Log.d(weatherData[x].getDate(),weatherData[x].getWeather());
                    //If today is in the weather data, check to see it was forecasted to be 'Clear'. If yes, send notification.
                    if(weatherData[x].getDate().equals(today) && weatherData[x].getWeather().equals("Clear")){
                        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        Intent timeIntent = new Intent(context, MainActivity.class);
                        PendingIntent timePendingIntent = PendingIntent.getActivity(context, WEATHER_NOTIFICATION_ID, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                        //send notification
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WEATHER_CHANNEL_ID);
                        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_big_icon);
                        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setLargeIcon(bm)
                                .setContentTitle(context.getString(R.string.weather_notification_title))
                                .setContentText(context.getString(R.string.weather_notfication_message))
                                .setContentIntent(timePendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_ALL);

                        nm.notify(WEATHER_NOTIFICATION_ID, builder.build());
                    }
                }

            }
            return null;
        }
    }


}