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

// Shows the history of old sessions recorded
public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private ListView listView;
    private Context context;
    // File for storing the old sessions recorded
    private final String fileName = "progress.txt";

    // On the creation of the view a list view containing old sessions recorded is shown
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProgressViewModel dashboardViewModel =
                new ViewModelProvider(this).get(ProgressViewModel.class);
        context = MainActivity.getContext();
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Reads old sessions recorded and stores it in infoMatches
        ArrayList<String> infoMatches = new ArrayList<>();
        ArrayList<String> oldSessions = readProgressFromFile(infoMatches);

        // Dynamic List view that show the old sessions
        listView = binding.listViewProgress;
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                infoMatches
        );
        listView.setAdapter(arrayAdapter);

        // Listener on each item of the list view, when one is clicked the resultFragment is shown
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // A bundle is prepared for passing information about the session selected
                Bundle bundle = new Bundle();
                bundle.putString("info", oldSessions.get(position));
                Navigation.findNavController(view).navigate(R.id.resultFragment, bundle);

            }
        });

        return root;
    }

    // Reads information of old sessions
    private ArrayList<String> readProgressFromFile(ArrayList<String> infoMatches){

        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, fileName);
        ArrayList<String> oldSessions = new ArrayList<String>();
        if(!readFrom.exists()){
            return oldSessions;
        }

        // Reads file line by line and gets only the date and duration of old sessions
        try (BufferedReader br = new BufferedReader(new FileReader(readFrom))) {
            String line;
            while ((line = br.readLine()) != null) {
                oldSessions.add(line);
                String[] parts = line.split(";");
                // parts[1] = date
                // parts[2] = duration
                infoMatches.add(parts[1] + "  " + parts[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return oldSessions;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}