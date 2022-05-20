package com.example.mypadel;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DataCollection extends Service {

    private String TAG = "DataCollection";
    private String storagePath = "/storage/emulated/0/Android/data/com.example.mypadel/files";
    private ChannelClient.Channel channel;
    private InputStream inputStream;
    private MyCallback myCallback;
    private ChannelClient channelClient;
    private Context context;
    MutableLiveData<Boolean> listen = new MutableLiveData<>();
    private int SIZEOF_LONG = 8;
    private int SIZEOF_FLOAT = 4;
    private String fileName;
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
        storagePath = context.getExternalFilesDir(null).toString();
        channelClient = Wearable.getChannelClient(context);
        myCallback = new MyCallback();

        Task<Void> regChannelTask = channelClient.registerChannelCallback(myCallback);
        regChannelTask.addOnCompleteListener(task -> {
            if(task.isSuccessful())
                Log.i(TAG, "The callback has been registered");
        });

        listen.observeForever(aBoolean -> {
            if(aBoolean) {
                Runnable toRun = () -> {
                    receiveData();
                };
                Thread executingThread = new Thread(toRun);
                executingThread.start();
            } else {
                Log.i(TAG, "listen is set to false");

                //stop chronometer
                Intent intent1 = new Intent("UpdateGui");
                intent1.putExtra("Chronometer", "stop");
                context.sendBroadcast(intent1);

                //DISABLING INPUT
                intent1 = new Intent("not_touchable");
                intent1.putExtra("touchable", true);
                context.sendBroadcast(intent1);
            }
        });
    }

    private void receiveData(){
        while(listen.getValue()) {
            byte[] array = new byte[1000*(SIZEOF_LONG + 4*SIZEOF_FLOAT)];
            int letti = 0;
            try {
                letti = inputStream.read(array);
            } catch (IOException e) {
                e.printStackTrace();
                listen.setValue(false);
            }

            //start the chronometer after we received the first batch of data
            if(firstTime){
                Intent intent = new Intent("UpdateGui");
                intent.putExtra("Chronometer", "start");
                getApplicationContext().sendBroadcast(intent);
                firstTime = false;

                //DISABLING INPUT
                intent = new Intent("not_touchable");
                intent.putExtra("touchable", false);
                getApplicationContext().sendBroadcast(intent);
            }
            ByteBuffer bb = ByteBuffer.wrap(array);
            float dataType;
            StringBuilder content = new StringBuilder();
            for(int i = 0; i < letti; i+=24){
                dataType = bb.getFloat();
                if(dataType == 2.0f){
                    Log.i(TAG, "The wearable has stopped to send data");
                    //end of the session, now we can classify
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
            writeToFile(fileName, content.toString());
        }
    }

    private void writeToFile(String fileName, String content){
        try {
            File f = new File(storagePath, fileName);
            FileWriter fw = new FileWriter(f, true);
            fw.append(content);
            fw.close();
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
            super.onChannelOpened(c);
            Log.i(TAG, "A channel has been established");
            channel = c;
            Task<InputStream> in_task = channelClient.getInputStream(channel);
            in_task.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    inputStream = task.getResult();
                    listen.postValue(true);
                } else {
                    Log.i(TAG, "Error retrieving inputStream " + task.getException().toString());
                }
            });
        }
        @Override
        public void onChannelClosed(ChannelClient.Channel c, int closeReason, int appError){
            Log.i(TAG, "onChannelclosed: " + closeReason + " " + appError);
        }
        @Override
        public void onInputClosed(ChannelClient.Channel c, int closeReason, int appError){
            Log.i(TAG, "onInputclosed: " + closeReason + " " + appError);
        }
        @Override
        public void onOutputClosed(ChannelClient.Channel c, int closeReason, int appError){
            Log.i(TAG, "onOutputclosed: " + closeReason + " " + appError);
        }
    }
}