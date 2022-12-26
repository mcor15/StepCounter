package com.example.step_counter;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

//RoomDatabase for step history.
@Database(entities = {Date.class}, version = 2, exportSchema = false)
public abstract class HistoryRoomDatabase extends RoomDatabase {
    private static HistoryRoomDatabase INSTANCE;
    public abstract HistoryDao historyDao();

    public static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback(){
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db){
            super.onOpen(db);
            new PopulateDbAsync(INSTANCE).execute();
        }
    };


    public static HistoryRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (HistoryRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                    //HistoryRoomDatabase.class, "history_database").fallbackToDestructiveMigration().addCallback(sRoomDatabaseCallback).build();
                    HistoryRoomDatabase.class, "history_database").fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

    //Populate database for testing
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void>{
        private final HistoryDao mDao;
        //Test data for populate database
        String[] dates = {"26/02/2022","27/02/2022","28/02/2022","02/03/2022","03/03/2022"};
        int[] steps = {5000, 2500, 3000, 0, 150,};

        PopulateDbAsync(HistoryRoomDatabase db){
            mDao = db.historyDao();
        }
        @Override
        protected Void doInBackground(final Void... prams) {
            if(mDao.getAnyDate().length <1){
                for(int i=0; i <dates.length; i++){
                    Date date = new Date(dates[i], steps[i]);
                    mDao.insert(date);
                }
            }
            return null;
        }
    }






}
