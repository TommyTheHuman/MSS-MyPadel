package com.example.mypadel;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.io.Serializable;

public class SensedData implements Serializable {
    public float dataSource; //0 if the data come from the accelerometer, 1 if gyroscope
    public long timestamp;
    public float[] values; //sensed values on the three axys

    SensedData(SensorEvent sensorEvent){
        values = new float[3];
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            dataSource = 0;
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            dataSource = 1;
        timestamp = sensorEvent.timestamp;
        System.arraycopy(sensorEvent.values, 0, values, 0, 3);
    }
}
