package com.example.mypadel.ui.progress;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mypadel.MainActivity;
import com.example.mypadel.R;
import com.example.mypadel.ResultFragment;
import com.example.mypadel.databinding.*;

import org.checkerframework.checker.units.qual.A;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private ListView listView;
    private Context context;
    private final String fileName = "progress.txt";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProgressViewModel dashboardViewModel =
                new ViewModelProvider(this).get(ProgressViewModel.class);
        context = MainActivity.getContext();
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ArrayList<String> infoMatches = new ArrayList<>();
        ArrayList<String> oldSessions = readProgressFromFile(infoMatches);
        listView = binding.listViewProgress;

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                infoMatches
        );

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Bundle bundle = new Bundle();
                bundle.putString("info", oldSessions.get(position));
                Navigation.findNavController(view).navigate(R.id.resultFragment, bundle);

            }
        });
         //final TextView textView = binding.textProgress;
        //dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        //Navigation.findNavController(this.getView()).navigate(R.id.navigation_dashboard);
        return root;
    }

    private ArrayList<String> readProgressFromFile(ArrayList<String> infoMatches){
        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, fileName);
        ArrayList<String> oldSessions = new ArrayList<String>();
        if(!readFrom.exists()){
            return oldSessions;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(readFrom))) {
            String line;
            while ((line = br.readLine()) != null) {
                oldSessions.add(line);
                String[] parts = line.split(";");
                infoMatches.add(parts[1] + "  " + parts[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return oldSessions;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}