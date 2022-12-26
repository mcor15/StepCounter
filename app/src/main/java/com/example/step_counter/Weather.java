package com.example.step_counter;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

//Data entity for weather db
@Entity(tableName = "weather_table")
public class Weather {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name="Date")
    private String date;

    @ColumnInfo(name = "Weather")
    private String weather;

    public Weather(@NonNull String date, @NonNull String weather){
        this.date =date;
        this.weather=weather;
    }

    public String getDate(){
        return this.date;
    }

    public String getWeather(){
        return this.weather;
    }

    public void setWeather(String weather){
        this.weather=weather;
    }

}
