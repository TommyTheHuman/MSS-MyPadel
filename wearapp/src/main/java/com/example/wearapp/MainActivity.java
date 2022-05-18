package com.example.wearapp;

import static android.content.Context.SENSOR_SERVICE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import com.example.wearapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends FragmentActivity implements SensorEventListener, AmbientModeSupport.AmbientCallbackProvider {

    private TextView mTextView;
    private ActivityMainBinding binding;
    private String TAG = "OROLOGIO";
    private String smartphoneID = "b1887e3a";
    private String communicationPath = "MyPadel";
    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private int SIZEOF_LONG = 8;
    private int SIZEOF_FLOAT = 4;
    private int SIZEOF_INT = 4;
    private Button button;
    //private List<SensedData> dataList = new ArrayList<>();
    private DataList dataList = new DataList();
    private final long FIVE_SECONDS_IN_NANOS = 5000000000L;
    private AmbientModeSupport.AmbientController ambientController;
    private int conta = 0;
    private ChannelClient.Channel channel = null;
    private OutputStream outputStream;
    private String storagePath = "/storage/emulated/0/Android/data/com.example.wearApp/files";
    private BroadcastReceiver broadcastReceiver;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Log.i(TAG, String.valueOf(getApplicationContext().getExternalFilesDir(null)));
        //commentato perche lo fa il service
        /*sensorSetup();
        setSmartphoneID();*/
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(view -> {
            if("Start".contentEquals(button.getText())) {
                //commento perche lo fa il service (da controllare)
                //registerSensorListener();

                Intent intent = new Intent(this, SensorHandler.class);
                intent.setAction("start_sensors");
                startService(intent);
                //send a message to start gathering data
                chronometer = (Chronometer) findViewById(R.id.chronometer);
                doResetBaseTime();
                chronometer.start();
                button.setText(R.string.button_stop);
            }
            else {
                button.setText(R.string.communication_end);
                chronometer.stop();
                button.setEnabled(false);
                //send a specific message to start the classification
                //commento perche lo fa il service (da controllare)
                //sendPacket(true);
                //unregisterSensorListener();
                Intent intent = new Intent(this, SensorHandler.class);
                intent.setAction("stop_sensors");
                startService(intent);

            }
        });
        button.setEnabled(false);
        Log.i(TAG, "listener bottone ok");

        //ambient mode
        ambientController = AmbientModeSupport.attach(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //

        //creazione canale
        //commentato perchè lo fa il service (da controllare)
        /*
        ChannelClient channelClient = Wearable.getChannelClient(getApplicationContext());
        Task<ChannelClient.Channel> ch_task = channelClient.openChannel(smartphoneID, communicationPath);
        ch_task.continueWithTask(new Continuation<ChannelClient.Channel, Task<OutputStream>>() {
            @Override
            public Task<OutputStream> then(@NonNull Task<ChannelClient.Channel> task) throws Exception {
                channel = task.getResult();
                return channelClient.getOutputStream(channel);
            }
        }).addOnSuccessListener(new OnSuccessListener<OutputStream>() {
            @Override
            public void onSuccess(OutputStream newOutputStream) {
                Log.i(TAG, "successo");
                outputStream = newOutputStream;
                button.setEnabled(true);
                Log.i(TAG, "Successo");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "Fallimento: " + e.toString());
            }
        });*/
        //starting ChannelHandler service
        Intent intent = new Intent(this, ChannelHandler.class);
        intent.setAction("create_channel");
        startService(intent);


        //registro broadcast receiver per ricevere intent utili a aggiornamento UI
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive della broadcast");
                if(intent.getAction() != null && intent.getAction().equals("update_button")){
                    Log.i(TAG, "dentro if");
                    boolean enabling = intent.getBooleanExtra("button", false);
                    button = (Button) findViewById(R.id.button);
                    button.setEnabled(enabling);
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("update_button"));
    }

    private void doResetBaseTime()  {
        // Returns milliseconds since system boot, including time spent in sleep.
        long elapsedRealtime = SystemClock.elapsedRealtime();
        // Set the time that the count-up timer is in reference to.
        Log.i(TAG, "1");
        chronometer.setBase(elapsedRealtime);
        Log.i(TAG, "2");
    }

    private void sensorSetup(){
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Log.i(TAG, "accelerometer max FIFO: " + accelerometer.getFifoMaxEventCount());
        Log.i(TAG, "gyroscope max FIFO: " + gyroscope.getFifoMaxEventCount());
    }

    private void registerSensorListener(){
        sm.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if(accelerometer == null) {
            Log.d(TAG, "Accelerometer unavailable");
            finish();
        }
        sm.registerListener((SensorEventListener) this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        if(gyroscope == null) {
            Log.d(TAG, "Gyroscope unavailable");
            finish();
        }
    }

    private void unregisterSensorListener(){
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        /*Log.i(TAG, String.valueOf(sensorEvent.timestamp) + ": ");
        for (float f : sensorEvent.values)
            Log.i(TAG, String.valueOf(f));*/
        if(sensorEvent.sensor.getType() != Sensor.TYPE_GYROSCOPE && sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        dataList.addElement(new SensedData(sensorEvent));
        if(dataList.getList().get(dataList.getList().size() - 1).timestamp - dataList.getList().get(0).timestamp > FIVE_SECONDS_IN_NANOS)
            //Log.i(TAG, "invio " + dataList.getList().get(dataList.getList().size() - 1).timestamp + "|" + dataList.getList().get(0).timestamp);
            sendPacket(false);
    }

    private void sendPacket(Boolean end) {
        conta += dataList.getList().size();
        Log.i(TAG, "number of sent packets = " + conta);
        Log.i(TAG, "data: " + dataList.getList().get(0).timestamp + "|" + dataList.getList().get(0).values[1]);
        ByteBuffer tosend = createByteBuffer(end);
        try {
            outputStream.write(tosend.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
        tosend.clear();
        dataList.getList().clear();
    }

    private ByteBuffer createByteBuffer(Boolean end){
        ByteBuffer bb = ByteBuffer.allocate((SIZEOF_LONG + SIZEOF_FLOAT * 4) * dataList.getList().size());
        for(SensedData sd: dataList.getList()){
            if (sd.dataSource == 0)
                bb.putFloat(0);
            else if (sd.dataSource == 1)
                bb.putFloat(1);
            else if (end == true){
                bb.putFloat(2);
            }
            bb.putLong(sd.timestamp);
            for (float val : sd.values)
                bb.putFloat(val);
        }
        return bb;
    }

    private void writeToFile(String fileName, String content){
        try {
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i(TAG, "Accuracy changed");
    }

    private void setSmartphoneID(){
        //trovare id dei nodi
        Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
        wearableList.addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                List<Node> list = task.getResult();
                boolean trovato = false;
                if(list.size() != 1){
                    Log.i(TAG, "More than 1 node are connected");
                    Log.d(TAG, "size: " + list.size());
                    for(Node n: list) {
                        Log.d(TAG, "nome: " + n.getDisplayName());
                        if (!n.getDisplayName().contains("WATCH")) {
                            smartphoneID = n.getId();
                            trovato = true;
                            Log.i(TAG, "Id of the smartphone obtained succesfully");
                            break;
                        }
                    }
                    if(!trovato) {
                        Toast.makeText(getApplicationContext(), "More than 1 node are connected", Toast.LENGTH_SHORT).show();
                        button.setEnabled(false);
                        button.setText("ERROR");
                    }
                } else {
                    Log.i(TAG, "Id of the smartphone obtained succesfully");
                    smartphoneID = list.get(0).getId();
                }
            }
        });
    }

    //ambient mode
    @Override
    protected void onPause(){
        Log.i(TAG, "in pausa");
        super.onPause();
        //unregisterSensorListener();
        /*Intent intent = new Intent(this, SensorHandler.class);
        intent.setAction("stop_sensors");
        startService(intent);*/
    }

    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        Intent intent = new Intent(this, SensorHandler.class);
        intent.setAction("stop_sensors");
        startService(intent);

        intent = new Intent(this, ChannelHandler.class);
        intent.setAction("stop_service");
        startService(intent);
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            Log.i(TAG, "Enter ambient mode");
            super.onEnterAmbient(ambientDetails);
            button.getPaint().setAntiAlias(false);
            //onExitAmbient();
        }
        @Override
        public void onExitAmbient(){
            Log.i(TAG, "Exit ambient mode");
            super.onExitAmbient();
            button.getPaint().setAntiAlias(true);
        }
    }
    //
}