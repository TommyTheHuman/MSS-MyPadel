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
import android.widget.Chronometer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mypadel.R;
import com.example.mypadel.StrokeClassification;
import com.example.mypadel.databinding.*;

public class ActivityFragment extends Fragment {

    private Chronometer chronometer;

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
        chronometer = (Chronometer) getView().findViewById(R.id.chronometer);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.hasExtra("Chronometer")){
                    String value = intent.getStringExtra("Chronometer");
                    if(value.equals("start")){
                        doResetBaseTime();
                        chronometer.start();
                    }else{
                        chronometer.stop();
                        sessionDuration = SystemClock.elapsedRealtime() - chronometer.getBase();
                        doResetBaseTime();
                        Log.i(TAG, String.valueOf(sessionDuration));

                        Intent intentClass = new Intent(context, StrokeClassification.class);
                        intentClass.setAction("Classify");
                        intentClass.putExtra("Duration", sessionDuration);
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
        chronometer.setBase(elapsedRealtime);
    }
}