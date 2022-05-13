package com.example.mypadel.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mypadel.DataCollection;
import com.example.mypadel.R;
import com.example.mypadel.databinding.*;

public class ActivityFragment extends Fragment {

    private Button start_button;
    private Chronometer chronometer;
    // variable to keep the state of activity -> false = not started, true = started
    private Boolean activityState = false;

    private FragmentActivityBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ActivityViewModel homeViewModel =
                new ViewModelProvider(this).get(ActivityViewModel.class);

        binding = FragmentActivityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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
}