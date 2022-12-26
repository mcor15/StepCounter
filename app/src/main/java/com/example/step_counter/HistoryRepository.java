package com.example.step_counter;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

//Repository for History Database.
public class HistoryRepository {
    private HistoryDao mHistoryDao;
    private LiveData<List<Date>> mStepHistory;

    HistoryRepository(Application application){
        HistoryRoomDatabase db = HistoryRoomDatabase.getDatabase(application);
        mHistoryDao = db.historyDao();
        mStepHistory = mHistoryDao.getHistory();
    }

    //return the entire step history
    LiveData<List<Date>> getHistory(){
        return mStepHistory;
    }

    //put a new steps row into database
    public void insert(Date date){ new insertAsyncTask(mHistoryDao).execute(date);}

    //update steps for date(row)
    public void update (Date date){
        new updateAsyncTask(mHistoryDao).execute(date);
    }

    //add steps to the current steps for the required date
    public void addSteps(Date date){new addStepsAsyncTask(mHistoryDao).execute(date);}

    //delete the entire database
    public void deleteAllHistory(){new deleteAllHistoryAsyncTask(mHistoryDao).execute();}

    //checks if a date already exits in the database
    public LiveData<Boolean> doesDateExist(String date){
       doesDateExistAsyncTask task =  new doesDateExistAsyncTask(mHistoryDao);
       task.execute(date);
       return task.mutRes;
    }

    //get steps for a given date
    public LiveData<Integer> getSteps(String date){
        getStepsAsyncTask task =  new getStepsAsyncTask(mHistoryDao);
        task.execute(date);
        return task.mutSteps;
    }

    //Support Async Tasks for database operations
    private static class updateAsyncTask extends AsyncTask<Date, Void, Void>{
        private HistoryDao mAsyncTaskDao;

        updateAsyncTask(HistoryDao dao){
            this.mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(Date... dates) {
            mAsyncTaskDao.update(dates[0]);
            return null;
        }
    }

    private static class getStepsAsyncTask extends AsyncTask<String, Void, Integer>{
        public MutableLiveData<Integer> mutSteps = new MutableLiveData<>();
        private HistoryDao mAsyncTaskDao;

        getStepsAsyncTask(HistoryDao dao){
            this.mAsyncTaskDao = dao;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            Integer steps = mAsyncTaskDao.getSteps(strings[0]);
            return steps;
        }

        @Override
        protected void onPostExecute(Integer result){
            mutSteps.postValue(result);
        }
    }

    private static class doesDateExistAsyncTask extends AsyncTask<String, Void, Boolean> {
        private HistoryDao mAsyncTaskDao;
        public MutableLiveData<Boolean> mutRes = new MutableLiveData<>();
        public int result;

        doesDateExistAsyncTask(HistoryDao dao){
            mAsyncTaskDao = dao;
        }


        @Override
        protected Boolean doInBackground(String... strings) {
            String date = strings[0];
            Boolean result = mAsyncTaskDao.doesDateExist(date);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result){
            mutRes.postValue(result);
        }
    }

    private static class addStepsAsyncTask extends AsyncTask<Date, Void, Void>{
        private HistoryDao mAsyncTaskDao;

        addStepsAsyncTask(HistoryDao dao){mAsyncTaskDao = dao;}

        @Override
        protected Void doInBackground(Date... dates) {
            String date = dates[0].getDate();
            int steps = dates[0].getSteps();
            mAsyncTaskDao.addSteps(date, steps);
            return null;
        }
    }
    
    private static class insertAsyncTask extends AsyncTask<Date, Void, Void>{
        private HistoryDao mAsyncTaskDao;
        
        insertAsyncTask(HistoryDao dao){
            mAsyncTaskDao = dao;
        }
        

        @Override
        protected Void doInBackground(final Date... dates) {
            mAsyncTaskDao.insert(dates[0]);
            return null;
        }
    }
    private static class deleteAllHistoryAsyncTask extends AsyncTask<Void, Void, Void>{
        private HistoryDao mAsyncTaskDao;

        deleteAllHistoryAsyncTask(HistoryDao dao){
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mAsyncTaskDao.deleteAllHistory();
            return null;
        }
    }


}
