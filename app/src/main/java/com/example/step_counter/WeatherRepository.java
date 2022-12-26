package com.example.step_counter;

import android.app.Application;

import androidx.lifecycle.LiveData;

//Weather repository
public class WeatherRepository {
    private WeatherDao weatherDao;
    private  LiveData<Weather[]> weather;

    WeatherRepository(Application application){
        WeatherRoomDatabase db = WeatherRoomDatabase.getDatabase(application);
        weatherDao= db.weatherDao();
        weather = weatherDao.getAsyncWeather();
    }

    LiveData<Weather[]> getWeather(){
        return weather;
    }


}
