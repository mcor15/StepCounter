package com.example.step_counter;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

//History View Model
public class HistoryViewModel extends AndroidViewModel {
    private HistoryRepository mRepository;
     private LiveData<List<Date>> mStepHistory;

     public HistoryViewModel(Application application){
         super(application);
         mRepository = new HistoryRepository(application);
         mStepHistory = mRepository.getHistory();
     }

     //return the entire step history
     LiveData<List<Date>> getHistory(){
         return mStepHistory;
     }

     //put a new steps row into database
     public void insert(Date date){
         mRepository.insert(date);
     }

     //update steps for date(row)
     public void update(Date date){mRepository.update(date);}

    //checks if a date already exits in the database
     public LiveData<Boolean> doesDateExist(String date){return mRepository.doesDateExist(date);}

    //add steps to the current steps for the required date
     public void addSteps(Date date){mRepository.addSteps(date);}

    //delete the entire database
     public void deleteAllHistory(){mRepository.deleteAllHistory();}


}
