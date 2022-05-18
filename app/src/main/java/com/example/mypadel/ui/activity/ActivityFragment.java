package com.example.mypadel.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mypadel.R;
import com.example.mypadel.StrokeClassification;
import com.example.mypadel.databinding.*;

public class ActivityFragment extends Fragment {

    private Button start_button;
    private Chronometer chronometer;
    // variable to keep the state of activity -> false = not started, true = started
    private Boolean activityState = false;
    private final String fileNameProgress = "progressi.txt";

    private BroadcastReceiver broadcastReceiver;
    private String TAG = "ActivityFragment";
    private long sessionDuration;

    private FragmentActivityBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ActivityViewModel homeViewModel =
                new ViewModelProvider(this).get(ActivityViewModel.class);

        binding = FragmentActivityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        registerBroadcastReceiver();
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        start_button = (Button) getView().findViewById(R.id.start_button);
        chronometer = (Chronometer) getView().findViewById(R.id.chronometer);

        start_button.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                if(!activityState) {
                                                    start_button.setText("STOP");
                                                    activityState = true;
                                                    chronometer.start();
                                                } else {
                                                    start_button.setText("START");
                                                    activityState = false;
                                                    // salva tutte cose in progressi
                                                    // capisci come resettare
                                                    // chronometer.setBase(0);
                                                }

                                                //DataCollection.startRecording();
                                            }
                                        }
        );

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void start_activity(){

    }

    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive della broadcast");
                if(intent.hasExtra("Chronometer")){
                    String value = intent.getStringExtra("Chronometer");
                    if(value.equals("start")){
                        doResetBaseTime();
                        Log.i(TAG, "pre start");
                        chronometer.start();
                        Log.i(TAG, "post start");
                    }else{
                        Log.i(TAG, "dentro fragment");
                        chronometer.stop();
                        Log.i(TAG, "cronometro stoppato");
                        sessionDuration = SystemClock.elapsedRealtime() - chronometer.getBase();
                        doResetBaseTime();
                        Log.i(TAG, String.valueOf(sessionDuration));

                        Intent intentClass = new Intent(context, StrokeClassification.class);
                        Log.i(TAG, "intent creato");
                        intentClass.setAction("Classify");
                        intentClass.putExtra("Duration", sessionDuration);
                        Log.i(TAG, "lancio servizio");
                        context.startService(intentClass);

                    }
                }
            }
        };
        requireActivity().registerReceiver(broadcastReceiver, new IntentFilter("UpdateGui"));
    }

    private void doResetBaseTime()  {
        // Returns milliseconds since system boot, including time spent in sleep.
        long elapsedRealtime = SystemClock.elapsedRealtime();
        // Set the time that the count-up timer is in reference to.
        Log.i(TAG, "1");
        chronometer.setBase(elapsedRealtime);
        Log.i(TAG, "2");
    }
}