package com.example.step_counter;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

//Dao for weather database
// Example table
//
// |Date-----Steps|
// |02/05/22|Clear|
// |06/07/23|Rain |
@Dao
public interface WeatherDao {

    //Insert a weather row
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Weather weather);

    //Get weather for all days in db.
    @Query("SELECT * FROM weather_table")
    Weather[] getWeather();

    //Delete all weather from db
    @Query("DELETE FROM weather_table")
    void deleteWeather();

    //Set a single weather row
    @Query("SELECT * FROM weather_table LIMIT 1")
    Weather getAWeather();

    //Abandoned
    @Query("SELECT * FROM weather_table")
    LiveData<Weather[]> getAsyncWeather();

}
