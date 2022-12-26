package com.example.step_counter;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

//Dao for History (Date) Database
// Example table
//
// |Date-----Steps|
// |02/05/22|526  |
// |06/07/23|2987 |
@Dao
public interface HistoryDao {

    //Insert a row
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Date date);

    //Update a row
    @Update
    void update(Date date);

    //delete all the rows
    @Query("DELETE FROM history_table")
    void deleteAll();

    //Pull the entire history
    @Query("SELECT * from history_table")
    LiveData<List<Date>> getHistory();

    //Get a history record
    @Query("SELECT * from history_table LIMIT 1")
    Date[] getAnyDate();

    //Get a Date
    @Query("SELECT * from history_table LIMIT 1")
    Date getOneDate();

    //Delete all history. Can't remember why there are two
    @Query("DELETE FROM history_table")
    void deleteAllHistory();

    // steps += new_steps for a select date
    @Query("UPDATE history_table SET Steps = Steps + :newSteps WHERE Date = :date")
    void addSteps(String date, int newSteps);

    //Query the DB to see is a date is in it.
    @Query("SELECT EXISTS(SELECT * from history_table WHERE history_table.Date = :date)")
    Boolean doesDateExist(String date);

    //Get steps for a given date.
    @Query("SELECT Steps FROM history_table WHERE Date = :date")
    int getSteps(String date);
}
