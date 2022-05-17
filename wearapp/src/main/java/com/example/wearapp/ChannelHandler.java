package com.example.wearapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
        Log.i(TAG, "onCreate service");
        super.onCreate();
        //setting the id of the connected smartphone and defining the channel with it
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
                        Toast.makeText(getApplicationContext(), "More than 1 node are connected", Toast.LENGTH_SHORT).show();
                        //button.setEnabled(false);
                        //button.setText("ERROR");
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
                    //button.setEnabled(true);
                    Log.i(TAG, "avvio bottone");
                    Intent intent = new Intent("update_button");
                    intent.putExtra("button", true);
                    getApplicationContext().sendBroadcast(intent);
                    Log.i(TAG, "Channel established succesfully");
                }).addOnFailureListener(e -> Log.i(TAG, "Fallimento: " + e.toString()));
            }
        });
        /*ChannelClient channelClient = Wearable.getChannelClient(getApplicationContext());
        Task<ChannelClient.Channel> ch_task = channelClient.openChannel(smartphoneID, communicationPath);
        ch_task.continueWithTask(task -> {
            channel = task.getResult();
            return channelClient.getOutputStream(channel);
        }).addOnSuccessListener(newOutputStream -> {
            Log.i(TAG, "successo");
            outputStream = (newOutputStream);
            //button.setEnabled(true);
            Log.i(TAG, "Successo");
        }).addOnFailureListener(e -> Log.i(TAG, "Fallimento: " + e.toString()));*/

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

    /*private void setSmartphoneID(){
        //trovare id dei nodi
        Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
        wearableList.addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                List<Node> list = task.getResult();
                boolean trovato = false;
                if(list.size() != 1){
                    Log.i(TAG, "More than 1 node are connected");
                    for(Node n: list) {
                        if (!n.getDisplayName().contains("WATCH")) {
                            smartphoneID = n.getId();
                            trovato = true;
                            Log.i(TAG, "Id of the smartphone obtained succesfully");
                            break;
                        }
                    }
                    if(!trovato) {
                        Toast.makeText(getApplicationContext(), "More than 1 node are connected", Toast.LENGTH_SHORT).show();
                        //button.setEnabled(false);
                        //button.setText("ERROR");
                    }
                } else {
                    Log.d(TAG, "Id of the smartphone obtained succesfully");
                    smartphoneID = list.get(0).getId();
                }
            }
        });
    }*/

    private void sendPacket(byte[] tosend) {
        try {
            outputStream.write(tosend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}