package com.example.mypadel.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.mypadel.MainActivity;
import com.example.mypadel.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mypadel.databinding.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private final String TAG = "Profile";
    private FragmentProfileBinding binding;
    private TextView name;
    private TextView height;
    private TextView weight;
    private Spinner spinner;

    private String spinner_content;
    private String name_content;
    private String height_content;
    private String weight_content;
    private Context context;

    private static String filePath;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel notificationsViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        context = MainActivity.getContext();
        filePath = context.getExternalFilesDir(null).toString();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity main = (MainActivity) getActivity();

        Spinner dropdown = getView().findViewById(R.id.spinner);
        //create a list of items for the spinner.
        String[] items = new String[]{"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(main, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);

        spinner = (Spinner) getView().findViewById(R.id.spinner);
        name = (TextView) getView().findViewById(R.id.name_filled);
        height = (TextView) getView().findViewById(R.id.height_filled);
        weight = (TextView) getView().findViewById(R.id.weight_filled);

        readFromFile("profile.txt",name, spinner, height, weight);



    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        spinner = (Spinner) getView().findViewById(R.id.spinner);
        spinner_content = spinner.getSelectedItem().toString();

        name = (TextView) getView().findViewById(R.id.name_filled);
        name_content = name.getText().toString();

        height = (TextView) getView().findViewById(R.id.height_filled);
        height_content = height.getText().toString();

        weight = (TextView) getView().findViewById(R.id.weight_filled);
        weight_content = weight.getText().toString();

        Log.i(TAG, "Name:" + name_content + "\n Level:" + spinner_content
              + "\n Height:" + height_content + "\n Weight:" + weight_content);

        writeToFile("profile.txt", name_content, spinner_content, height_content, weight_content);

        binding = null;
    }


    private void readFromFile(String fileName, TextView name, Spinner level, TextView height, TextView weight) {
        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, fileName);
        if(!readFrom.exists()){
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(readFrom))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if(!parts[0].equals("empty"))
                    name.setText(parts[0]);
                if(parts[1].equals("Beginner"))
                    spinner.setSelection(0);
                if(parts[1].equals("Intermediate"))
                    spinner.setSelection(1);
                if(parts[1].equals("Expert"))
                    spinner.setSelection(2);
                if(!parts[2].equals("empty"))
                    height.setText(parts[2]);
                if(!parts[3].equals("empty"))
                    weight.setText(parts[3]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(String fileName, String name, String level, String height, String weight){
        try {
            File f = new File(filePath, fileName);
            FileWriter fw = new FileWriter(f, false);
            if(name.equals(""))
                name = "empty";
            if(level.equals(""))
                level = "empty";
            if(height.equals(""))
                height = "empty";
            if(weight.equals(""))
                weight = "empty";

            String content = name + "," + level + "," + height + "," + weight;
            fw.append(content);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}