package com.example.mypadel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Chronometer;
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

public class DataCollection extends Service {

    private String TAG = "SMARTPHONE";
    private String wearableID = "da5c1706";
    private String communicationPath = "MyPadel";
    private String storagePath = "/storage/emulated/0/Android/data/com.example.mypadel/files";
    private int conta = 0;
    private ChannelClient.Channel channel;
    private InputStream inputStream;
    private ObjectOutputStream outputStream;
    private MyCallback myCallback;
    private ChannelClient channelClient;
    private DataList dataList = null;
    private StrokeClassification strokeClassifier = new StrokeClassification();
    private Context context;
    //private boolean listen = true; //CHANGE prima era true
    //CHANGE
    MutableLiveData<Boolean> listen = new MutableLiveData<>();
    //CHANGE
    private int SIZEOF_LONG = 8;
    private int SIZEOF_FLOAT = 4;
    private TextView contatoreView;
    private String fileName;
    private int counter = 5;
    private Chronometer chronometer;
    private Boolean firstTime = true;

    @Override
    public void onCreate() {
        context = MainActivity.getContext();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        if(intent.getAction() != null && intent.getAction().equals("start_recording")) {
            startRecording();
        } else if(intent.getAction() != null && intent.getAction().equals("stop_recording")){
            stopSelf();
        }

        return START_STICKY;

    }

    public void startRecording() {
        fileName = "data_collected.txt";
        File path = context.getExternalFilesDir(null);
        Log.i(TAG, path.toString());
        channelClient = Wearable.getChannelClient(context);
        myCallback = new MyCallback();

        Task<Void> regChannelTask = channelClient.registerChannelCallback(myCallback);
        regChannelTask.addOnCompleteListener(task -> {
            if(task.isSuccessful())
                Log.i(TAG, "The callback has been registered");
        });


        //contatoreView = (TextView) findViewById(R.id.contatore);
        //contatoreView.setText(String.valueOf(conta));
        //CHANGE
        listen.observeForever(aBoolean -> {
            if(aBoolean) {
                Log.i(TAG, "changed");
                /*
                //start chronometer
                chronometer = (Chronometer) findViewById(R.id.chronometer);

                chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                    @Override
                    public void onChronometerTick(Chronometer chronometer) {
                        if(counter <= 0) {
                            chronometer.stop();
                            chronometer.setOnChronometerTickListener(null);
                            doResetBaseTime();
                            chronometer.start();
                        }
                        chronometer.setText(counter + "");
                        counter--;
                    }
                });*/


                Runnable toRun = () -> {
                    receiveData();
                    Log.i(TAG, "thread creato");
                };
                Thread executingThread = new Thread(toRun);
                executingThread.start();
                Log.i(TAG, "thread partito");

            } else {
                Log.i(TAG, "listen is set to false, finish");

                //stop chronometer
                Intent intent1 = new Intent("UpdateGui");
                intent1.putExtra("Chronometer", "stop");
                context.sendBroadcast(intent1);


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

            //start the chronometer after we received the firtst batch of data

            if(firstTime){
                Intent intent = new Intent("UpdateGui");
                intent.putExtra("Chronometer", "start");
                getApplicationContext().sendBroadcast(intent);
                firstTime = false;
            }

            Log.i(TAG, "number of packets = " + letti/24);
            ByteBuffer bb = ByteBuffer.wrap(array);
            float dataType;
            StringBuilder content = new StringBuilder();
            for(int i = 0; i < letti; i+=24){
                dataType = bb.getFloat();
                if(dataType == 2.0f){
                    Log.i(TAG, "ricevuto stop");
                    //end of the session, now we can classify
                    //listen.setValue(false);
                    listen.postValue(false);
                    break;
                }
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
        Task<List<Node>> wearableList = Wearable.getNodeClient(context).getConnectedNodes();
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
            Toast.makeText(context, "Wrote to file " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFromFile(String fileName){
        File path = context.getFilesDir();
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
            Log.i(TAG, "onChannelOpened");
            super.onChannelOpened(c);
            Log.i(TAG, "A channel has been established");
            channel = c;
            Task<InputStream> in_task = channelClient.getInputStream(channel);
            in_task.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
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