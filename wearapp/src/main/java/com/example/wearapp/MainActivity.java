package com.example.wearapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

public class MainActivity extends FragmentActivity implements AmbientModeSupport.AmbientCallbackProvider {

    private String TAG = "MainActivity";
    private Button button;
    private AmbientModeSupport.AmbientController ambientController;
    private BroadcastReceiver broadcastReceiver;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(view -> {
            if("Start".contentEquals(button.getText())) {
                Intent intent = new Intent(this, SensorHandler.class);
                intent.setAction("start_sensors");
                startService(intent);
                chronometer = (Chronometer) findViewById(R.id.chronometer);
                doResetBaseTime();
                chronometer.start();
                button.setText(R.string.button_stop);
            }
            else {
                button.setText(R.string.communication_end);
                chronometer.stop();
                button.setEnabled(false);
                Intent intent = new Intent(this, SensorHandler.class);
                intent.setAction("stop_sensors");
                startService(intent);
            }
        });
        button.setEnabled(false);

        //ambient mode
        ambientController = AmbientModeSupport.attach(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //

        //starting ChannelHandler service
        Intent intent = new Intent(this, ChannelHandler.class);
        intent.setAction("create_channel");
        startService(intent);

        //registering a broadcast receiver in order to be able to update UI from the services
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals("update_button")){
                    boolean enabling = intent.getBooleanExtra("button", false);
                    button = (Button) findViewById(R.id.button);
                    button.setEnabled(enabling);
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("update_button"));
    }

    private void doResetBaseTime()  {
        // Returns milliseconds since system boot, including time spent in sleep.
        long elapsedRealtime = SystemClock.elapsedRealtime();
        // Set the time that the count-up timer is in reference to.
        chronometer.setBase(elapsedRealtime);
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        Intent intent = new Intent(this, SensorHandler.class);
        intent.setAction("stop_sensors");
        startService(intent);

        intent = new Intent(this, ChannelHandler.class);
        intent.setAction("stop_service");
        startService(intent);
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            Log.i(TAG, "Enter ambient mode");
            super.onEnterAmbient(ambientDetails);
            button.getPaint().setAntiAlias(false);
            //onExitAmbient();
        }
        @Override
        public void onExitAmbient(){
            Log.i(TAG, "Exit ambient mode");
            super.onExitAmbient();
            button.getPaint().setAntiAlias(true);
        }
    }
}