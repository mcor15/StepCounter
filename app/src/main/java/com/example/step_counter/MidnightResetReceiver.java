package com.example.step_counter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;


//Midnight Broadcast Receiver for saving steps from yesterday.
public class MidnightResetReceiver extends BroadcastReceiver {
    private static final int MIDNIGHT_BROADCAST_ID = 0;
    private static final String ACTION_MIDNIGHT_BROADCAST =
            BuildConfig.APPLICATION_ID + ".ACTION_MIDNIGHT_BROADCAST";


    public void onReceive(Context context, Intent intent) {
        //It has become a new day.
        Log.d("midnight receiver", "fired");

        //get sharedprefs
        SharedPreferences sp = context.getSharedPreferences("com.example.step_counter", Context.MODE_PRIVATE);
        int stepsForTheDay = sp.getInt("steps",0);

        //we know that data is from yesterday
        String day = DateHelper.getYesterday();

        //save steps to db
        UpdateDB updater = new UpdateDB(context, day, stepsForTheDay);
        updater.execute();

        //clear steps from PlayerPrefs
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();

        //Reset midnight alarm
        Intent alarmIntent = new Intent(context, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,MIDNIGHT_BROADCAST_ID, alarmIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,0);// set clock to 00.00 tomorrow
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.add(Calendar.DATE, 1);
        Log.d("MR midnight Alarm", String.valueOf(c.getTime()));

        //set alarm
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),pendingIntent);

        //Send broadcast to Main Activity. So, if it is open, deal with midnight transition.
        Intent intent2 = new Intent();
        intent2.setAction(ACTION_MIDNIGHT_BROADCAST);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent2);
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
}