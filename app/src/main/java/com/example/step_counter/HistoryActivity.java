package com.example.step_counter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

/*
History Activity for displaying user step history.
 */
public class HistoryActivity extends AppCompatActivity {

    private HistoryViewModel mHistoryViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        //Setup UI
        RecyclerView recyclerView = findViewById(R.id.history_recyclerview);
        final HistoryListAdapter adapter = new HistoryListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Get step database
        mHistoryViewModel = new  ViewModelProvider(this).get(HistoryViewModel.class);

        //connect recyclerView to step history data to display history
        mHistoryViewModel.getHistory().observe(this, new Observer<List<Date>>() {
            @Override
            public void onChanged(List<Date> dates) {
                adapter.setHistory(dates);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //setup menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        //clear history is selected from menu

        if(id == R.id.clear_history){
            Toast.makeText(this, "Clearing Step History...", Toast.LENGTH_LONG).show();
            mHistoryViewModel.deleteAllHistory();
        }


        return super.onOptionsItemSelected(item);
    }

}