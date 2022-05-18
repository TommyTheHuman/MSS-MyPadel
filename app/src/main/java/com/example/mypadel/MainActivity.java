package com.example.mypadel;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.example.mypadel.ui.progress.ProgressFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mypadel.databinding.ActivityMainBinding;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static Context context;
    private Chronometer chronometer;
    private BroadcastReceiver broadcastReceiver;
    private final String TAG = "MainActivity";
    private long sessionDuration = 0l;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();


        com.example.mypadel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_home, R.id.navigation_notifications)
                .build();
        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavHostFragment navHostFragment;
        navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


        Intent intent = new Intent(this, DataCollection.class);
        intent.setAction("start_recording");
        startService(intent);

        //registerBroadcastReceiver();
    }

    public static Context getContext(){
        return context;
        // or return instance.getApplicationContext();
    }

    /*
    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive della broadcast");
                if(intent.hasExtra("Chronometer")){
                    String value = intent.getStringExtra("Chronometer");
                    if(value.equals("start")){
                        //doResetBaseTime();
                        Log.i(TAG, "pre start");
                        chronometer.start();
                        Log.i(TAG, "post start");
                    }else{
                        chronometer.stop();
                        sessionDuration = SystemClock.elapsedRealtime() - chronometer.getBase();
                        doResetBaseTime();

                        Intent intentClass = new Intent(context, StrokeClassification.class);
                        intentClass.setAction("Classify");
                        intentClass.putExtra("Duration", sessionDuration);
                        startService(intentClass);

                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("UpdateGui"));
    }*/

    @Override
    public void onDestroy(){
        super.onDestroy();
        Intent intent = new Intent(this, StrokeClassification.class);
        intent.setAction("stopClassify");
        startService(intent);
    }

    private void doResetBaseTime()  {
        // Returns milliseconds since system boot, including time spent in sleep.
        long elapsedRealtime = SystemClock.elapsedRealtime();
        // Set the time that the count-up timer is in reference to.
        Log.i(TAG, "1");
        chronometer.setBase(elapsedRealtime);
        Log.i(TAG, "2");
    }

    public long getSessionDuration(){
        return sessionDuration;
    }
}
