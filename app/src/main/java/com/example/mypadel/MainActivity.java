package com.example.mypadel;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mypadel.databinding.ActivityMainBinding;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private static Context context;
    private final String TAG = "MainActivity";
    private long sessionDuration = 0l;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        int[] totStrokesClassified = new int[4];
        Log.d(TAG, Arrays.toString(totStrokesClassified));

        com.example.mypadel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_home, R.id.navigation_notifications)
                .build();

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

    @Override
    public void onDestroy(){
        super.onDestroy();
        Intent intent = new Intent(this, StrokeClassification.class);
        intent.setAction("stopClassify");
        startService(intent);

        intent = new Intent(this, DataCollection.class);
        intent.setAction("stop_recording");
        startService(intent);
    }


    public long getSessionDuration(){
        return sessionDuration;
    }
}
