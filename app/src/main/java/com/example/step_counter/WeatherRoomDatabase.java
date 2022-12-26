package com.example.step_counter;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

//RoomDatabase for weather
@Database(entities = {Weather.class}, version = 1, exportSchema = false)
public abstract class WeatherRoomDatabase extends RoomDatabase {
    private static WeatherRoomDatabase INSTANCE;
    public abstract WeatherDao weatherDao();


    public static WeatherRoomDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (WeatherRoomDatabase.class){
                if(INSTANCE == null){
                   INSTANCE = Room.databaseBuilder(context.getApplicationContext(), WeatherRoomDatabase.class,
                            "weather_database").fallbackToDestructiveMigration().build();

                }

            }
        }
        return INSTANCE;
    }


}
