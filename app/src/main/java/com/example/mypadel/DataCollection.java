package com.example.mypadel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class DataCollection extends AppCompatActivity {

    private String TAG = "SMARTPHONE";
    private String wearableID = "da5c1706";
    private String communicationPath = "MyPadel";
    private String storagePath = "/storage/emulated/0/Android/data/com.example.channelmobilewearable/files";
    private int conta = 0;
    private ChannelClient.Channel channel;
    private InputStream inputStream;
    private ObjectOutputStream outputStream;
    private MyCallback myCallback;
    private ChannelClient channelClient;
    private DataList dataList = null;
    //private boolean listen = true; //CHANGE prima era true
    //CHANGE
    MutableLiveData<Boolean> listen = new MutableLiveData<>();
    //CHANGE
    private int SIZEOF_LONG = 8;
    private int SIZEOF_FLOAT = 4;
    private TextView contatoreView;
    private String fileName;

    public void startRecording() {
        fileName = "data_collected.txt";
        File path = getApplicationContext().getExternalFilesDir(null);
        Log.i(TAG, path.toString());
        channelClient = Wearable.getChannelClient(getApplicationContext());
        myCallback = new MyCallback();

        Task<Void> regChannelTask = channelClient.registerChannelCallback(myCallback);
        regChannelTask.addOnCompleteListener(task -> {
            if(task.isSuccessful())
                Log.i(TAG, "The callback has been registered");
        });

        //notifico arrivo su smartphone
        Toast.makeText(getApplicationContext(), "Contatore: " + conta, Toast.LENGTH_SHORT).show();
        //contatoreView = (TextView) findViewById(R.id.contatore);
        //contatoreView.setText(String.valueOf(conta));
        //CHANGE
        listen.observeForever(aBoolean -> {
            if(aBoolean) {
                Log.i(TAG, "changed");
                receiveData();
            } else {
                Log.i(TAG, "listen is set to false, finish");
                finish();
            }
        });
        //CHANGE
    }

    private void receiveData(){
        while(listen.getValue()) {
            byte[] array = new byte[1000*(SIZEOF_LONG + 4*SIZEOF_FLOAT)];
            int letti = 0;
            try {
                letti = inputStream.read(array);
            } catch (IOException e) {
                e.printStackTrace();
                //CHANGE
                listen.setValue(false);
                //listen = false;
                //CHANGE
            }
            Log.i(TAG, "number of packets = " + letti/24);
            ByteBuffer bb = ByteBuffer.wrap(array);
            float dataType;
            StringBuilder content = new StringBuilder();
            for(int i = 0; i < letti; i+=24){
                dataType = bb.getFloat();
                content.append(dataType);
                content.append(";");
                content.append(bb.getLong());
                content.append(";");
                for (int j = 0; j < 3; j++) {
                    content.append(bb.getFloat());
                    content.append(";");
                }
                content.append("\n");
            }
            conta += (letti/24);
            writeToFile(fileName, content.toString());
            Log.i(TAG, "conta = " + conta);
        }
    }

    protected void logNodeList(){
        //trovare id dei nodi
        Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
        wearableList.addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Log.i(TAG, "nodi finito");
                List<Node> list = task.getResult();
                for(Node i: list){
                    Log.i(TAG, "nodi: " + i.getId());
                }
            }
        });
    }

    private void writeToFile(String fileName, String content){
        try {
            /*FileOutputStream writer = openFileOutput(fileName, MODE_APPEND);
            writer.write(content.getBytes());
            writer.close();*/
            File f = new File(storagePath, fileName);
            FileWriter fw = new FileWriter(f, true);
            fw.append(content);
            fw.close();
            // Toast fa vedere un pop up con scritto un messaggio
            Toast.makeText(getApplicationContext(), "Wrote to file " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFromFile(String fileName){
        File path = getApplicationContext().getFilesDir();
        File readFrom = new File(path, fileName);
        byte[] content = new byte[(int) readFrom.length()];

        try {
            FileInputStream stream = new FileInputStream(readFrom);
            stream.read(content);
            return new String(content);
        } catch (Exception e){
            e.printStackTrace();
            return e.toString();
        }
    }

    private class MyCallback extends ChannelClient.ChannelCallback {
        @Override
        public void onChannelOpened(@NonNull ChannelClient.Channel c){
            super.onChannelOpened(c);
            Log.i(TAG, "A channel has been established");
            channel = c;
            Task<InputStream> in_task = channelClient.getInputStream(channel);
            in_task.addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    inputStream = task.getResult();
                    Log.i(TAG, "Input stream ok");
                    //CHANGE
                    listen.postValue(true);
                    //receiveData();
                    //CHANGE
                } else {
                    Log.i(TAG, "Error retrieving inputStream " + task.getException().toString());
                }
            });/*
            Task<OutputStream> out_task = channelClient.getOutputStream(channel);
            out_task.addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    try {
                        outputStream = new ObjectOutputStream(task.getResult());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "Output stream OK");
                    receiveData();
                } else {
                    Log.i(TAG, "Error retrieving outputStream " + task.getException().toString());
                }
            });*/
        }
        @Override
        public void onChannelClosed(ChannelClient.Channel c, int closeReason, int appError){
            //listen = false;
            Log.i(TAG, "onChannelclosed: " + closeReason + " " + appError);
        }
        @Override
        public void onInputClosed(ChannelClient.Channel c, int closeReason, int appError){
            //listen = false;
            Log.i(TAG, "onInputclosed: " + closeReason + " " + appError);
        }
        @Override
        public void onOutputClosed(ChannelClient.Channel c, int closeReason, int appError){
            //listen = false;
            Log.i(TAG, "onOutputclosed: " + closeReason + " " + appError);
        }
    }
}