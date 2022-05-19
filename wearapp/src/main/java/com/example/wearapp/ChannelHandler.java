package com.example.wearapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ChannelHandler extends Service {
    private ChannelClient.Channel channel;
    private OutputStream outputStream;
    private String smartphoneID;
    private String communicationPath = "MyPadel";

    private final String TAG = "ChannelHandler";

    public ChannelHandler() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        //setting the id of the connected smartphone and defining the channel with it
        Runnable toRun = () -> {
            //setting smartphoneID
            Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            wearableList.addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    List<Node> list = task.getResult();
                    boolean trovato = false;
                    if(list.size() == 0){
                        Log.d(TAG, "No node has been found");
                    }
                    if(list.size() != 1){
                        Log.i(TAG, "More than 1 node are connected");
                        for(Node n: list) {
                            if (!n.getDisplayName().contains("WATCH")) {
                                smartphoneID = n.getId();
                                trovato = true;
                                Log.i(TAG, "Id of the smartphone obtained succesfully: " + n.getDisplayName());
                                break;
                            }
                        }
                        if(!trovato) {
                            //the button has to be disabled because there are more than 1 node connected
                            Log.d(TAG, "Unable to detect the unique smartphone");
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setAction("update_button");
                            intent.putExtra("button", false);
                            getApplicationContext().sendBroadcast(intent);
                        }
                    } else {
                        Log.d(TAG, "Id of the smartphone obtained succesfully: " + list.get(0).getDisplayName());
                        smartphoneID = list.get(0).getId();
                    }
                    //setting up the channel
                    ChannelClient channelClient = Wearable.getChannelClient(getApplicationContext());
                    Task<ChannelClient.Channel> ch_task = channelClient.openChannel(smartphoneID, communicationPath);
                    ch_task.continueWithTask(task2 -> {
                        channel = task2.getResult();
                        return channelClient.getOutputStream(channel);
                    }).addOnSuccessListener(newOutputStream -> {
                        outputStream = (newOutputStream);
                        //the button can be enabled because we are ready to send data
                        Intent intent = new Intent("update_button");
                        intent.putExtra("button", true);
                        getApplicationContext().sendBroadcast(intent);
                        Log.i(TAG, "Channel established succesfully");
                    }).addOnFailureListener(e -> Log.i(TAG, "Task failure: " + e.toString()));
                }
            });
        };
        Thread run = new Thread(toRun);
        run.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        if(intent.getAction() != null && intent.getAction().equals("send_data")){
            Runnable toRun = () -> {
                byte[] dataToSend = intent.getByteArrayExtra("toSend");
                sendPacket(dataToSend);
            };
            Thread run = new Thread(toRun);
            run.start();
        } else if(intent.getAction() != null && intent.getAction().equals("stop_service")){
            stopSelf();
        }
        return START_STICKY;
    }

    private void sendPacket(byte[] tosend) {
        try {
            outputStream.write(tosend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}