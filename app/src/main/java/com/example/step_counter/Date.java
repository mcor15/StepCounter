package com.example.step_counter;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

//Date entity for step history db
@Entity(tableName = "history_table")
public class Date {



    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "Date")
    private String mDate;

    @ColumnInfo(name = "Steps")
    private int mSteps;


    public Date(@NonNull String date, @NonNull int steps){
        this.mDate = date;
        this.mSteps = steps;
    }


    public String getDate(){
        return this.mDate;
    }


    public int getSteps(){
        return this.mSteps;
    }

    public void setSteps(int steps){
        this.mSteps = steps;
    }

}
