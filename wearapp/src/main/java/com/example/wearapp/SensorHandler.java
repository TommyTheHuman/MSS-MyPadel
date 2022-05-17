package com.example.wearapp;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SensorHandler extends Service implements SensorEventListener {
    private final String TAG = "SensorHandler";
    private List<SensedData> dataList = new ArrayList<>();
    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Thread executingThread;

    private final long FIVE_SECONDS_IN_NANOS = 5000000000L;
    private final int SIZEOF_LONG = 8;
    private final int SIZEOF_FLOAT = 4;

    public SensorHandler() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() != null && intent.getAction().equals("start_sensors")) {
            Runnable toRun = () -> {
                sensorSetup();
                registerSensorListener();
                Log.i(TAG, "thread creato");
            };
            executingThread = new Thread(toRun);
            executingThread.start();
            Log.i(TAG, "thread partito");
        } else if(intent.getAction() != null && intent.getAction().equals("stop_sensors")) {
            executingThread.stop();
            sendPacket(true);
        }

        return START_STICKY;
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
        }
        sm.registerListener((SensorEventListener) this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        if(gyroscope == null) {
            Log.d(TAG, "Gyroscope unavailable");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, String.valueOf(sensorEvent.timestamp) + ": ");
        for (float f : sensorEvent.values)
            Log.i(TAG, String.valueOf(f));
        if(sensorEvent.sensor.getType() != Sensor.TYPE_GYROSCOPE && sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        dataList.add(new SensedData(sensorEvent));
        if(dataList.get(dataList.size() - 1).timestamp - dataList.get(0).timestamp > FIVE_SECONDS_IN_NANOS)
            sendPacket(false);
    }

    private void sendPacket(Boolean end) {
        ByteBuffer tosend = createByteBuffer(end);
        Intent intent = new Intent(this, ChannelHandler.class);
        intent.setAction("send_data");
        intent.putExtra("toSend", tosend.array());
        startService(intent);
        tosend.clear();
        dataList.clear();
    }

    private ByteBuffer createByteBuffer(Boolean end){
        ByteBuffer bb = ByteBuffer.allocate((SIZEOF_LONG + SIZEOF_FLOAT * 4) * dataList.size());
        for(SensedData sd: dataList){
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { Log.i(TAG, "Accuracy changed"); }
}