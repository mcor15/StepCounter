package com.example.step_counter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Set;

//Worker to download weather information for when device is not connected to the internet.
public class WeatherDownloadWorker extends Worker {

    public WeatherDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }



    @NonNull
    @Override
    public Result doWork() {

        //Get device location.
        //Using LocationManager because can't use fusedLocationClient for some reason ¯\_(ツ)_/¯
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        String best = locationManager.getBestProvider(crit, false);
        @SuppressLint("MissingPermission") Location loc = locationManager.getLastKnownLocation(best);
        Log.d("loc",Double.toString(loc.getLatitude())+" "+Double.toString(loc.getLongitude()));

        //Build request uri. Only want daily 7 day forecast.
        String requestUrl = "https://api.openweathermap.org/data/2.5/onecall?lat="+Double.toString(loc.getLatitude())+"&lon="+Double.toString(loc.getLongitude())+"&exclude=current,minutely,hourly,alerts&appid="+BuildConfig.OPEN_WEATHER_KEY;
        Log.d("uri", requestUrl);

        //No url?
        if (requestUrl == null) {
            return Result.retry();
        }

        //Connection setup
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            //read data (JASON format)
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            String data = buffer.toString();
            Log.d("data->",buffer.toString());

            //Parse string to weather hash map with key being 'date' and value being 'weather condition'
            HashMap<String,String> weather = parseWeather(data);

            //Get weather db
            Context c = getApplicationContext();
            WeatherDao dao = WeatherRoomDatabase.getDatabase(c).weatherDao();
            Log.d("bool check", Boolean.toString(dao == null));

            //clear old weather
            dao.deleteWeather();

            //put new weather into db
            Set<String> keys = weather.keySet();
            for(String key : keys){
                dao.insert(new Weather(key, weather.get(key)));
            }
            Weather[] k = dao.getWeather();
            Log.d("k",Integer.toString(k.length));
            for(Weather w : k){
                Log.d(w.getDate(),w.getWeather());
            }
            return Result.success();


        } catch (MalformedURLException e) {
            e.printStackTrace();
            return Result.retry();
        } catch (IOException e) {
            e.printStackTrace();
            return Result.retry();
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
                return Result.failure();
            }

            //Download
            Log.d("doWork", "working");
            return Result.success();
        }
    }


    private HashMap<String,String> parseWeather(String data) throws JSONException {

        //Get 7 day forecast
        JSONObject jobs = new JSONObject(data);
        JSONArray daily = jobs.getJSONArray("daily");


        HashMap<String,String> weather = new HashMap<>();
        for(int x=0; x<daily.length(); x++){

            JSONObject current = (JSONObject) daily.get(x);

            //get date
            int unix_timestamp = current.getInt("dt");
            java.util.Date date = new java.util.Date((long)unix_timestamp*1000);//magic
            String pretty_date = new SimpleDateFormat("dd/MM/yyyy").format(date);

            //Get weather condition for date. Conditions can be:Thunderstorm, Drizzle, Rain, Snow, Clear, Clouds,
            // Mist, Smoke, Haze, Dust, Fog, Sand, Dust, Ash , Squall, Tornado
            JSONObject day_weather = (JSONObject)current.getJSONArray("weather").get(0);
            String weather_condition = day_weather.getString("main");

            //put date with predicted weather condition in HashMap
            weather.put(pretty_date, weather_condition);
        }
        return weather;
    }


}







