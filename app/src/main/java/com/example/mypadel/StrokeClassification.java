package com.example.mypadel;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.mypadel.ml.TfliteModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class StrokeClassification extends Service {

    private static final String TAG = "STROKE CLASSIFICATION";
    private static String filePath;
    // Original frequency was 100Hz, we reduce bu a factor of 4, so 25Hz
    private static final int reductionFactor = 4;
    // Empirically found threshold of the derivative of the accelerometer, to detect strokes
    private static final int userThreshold = 10;
    // Window of signal taken to classify the stroke
    private static final int userWindowSize = 30;
    // 2 strokes must be minimum 30 samples distant so 1.2s = (30/25Hz)
    private static final int userMinInterval = 30;
    private int SIZEOF_FLOAT = 4;
    private Context context;
    private long sessionDuration;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        context = MainActivity.getContext();
        filePath = context.getExternalFilesDir(null).toString();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        if(intent.getAction() != null && intent.getAction().equals("Classify")){
            sessionDuration = intent.getLongExtra("Duration", 0l);
            Runnable toRun = () -> {
                classifySession();
            };
            Thread run = new Thread(toRun);
            run.start();
        } else if(intent.getAction() != null && intent.getAction().equals("stopClassify")){
            stopSelf();
        }
        return START_STICKY;
    }

    public void classifySession(){
        String fileName = "data_collected.txt";

        // List of sensors' values per axis
        ArrayList<Float> xAcc = new ArrayList<>();
        ArrayList<Float> yAcc = new ArrayList<>();
        ArrayList<Float> zAcc = new ArrayList<>();
        ArrayList<Float> xGyr = new ArrayList<>();
        ArrayList<Float> yGyr = new ArrayList<>();
        ArrayList<Float> zGyr = new ArrayList<>();

        // List of x and y positions from the log
        ArrayList<Float> xPositions = new ArrayList<>();
        ArrayList<Float> yPositions = new ArrayList<>();

        // List of x and y positions of strokes
        ArrayList<Float> xStrokes = new ArrayList<>();
        ArrayList<Float> yStrokes = new ArrayList<>();

        // List of timestamps of position log
        ArrayList<Long> timestampPositions = new ArrayList<>();
        // List of timestamps of strokes
        ArrayList<Long> strokeTimestampPositions = new ArrayList<>();
        // List of all timestamps derived from sensors
        ArrayList<Long> allTimestamp = new ArrayList<>();
        // List of strokes predicted
        ArrayList<Integer> strokesPredicted = new ArrayList<>();
        // List of strokes predicted associated with log values
        ArrayList<Integer> strokesTypeLog = new ArrayList<>();


        // Read data from latest match recorded
        readSession(fileName, xAcc, yAcc, zAcc, xGyr, yGyr, zGyr, allTimestamp);

        // Frequency reduction of data
        ArrayList<Float> xAccRed = frequencyReduction(xAcc, reductionFactor);
        ArrayList<Float> yAccRed = frequencyReduction(yAcc, reductionFactor);
        ArrayList<Float> zAccRed = frequencyReduction(zAcc, reductionFactor);
        ArrayList<Float> xGyrRed = frequencyReduction(xGyr, reductionFactor);
        ArrayList<Float> yGyrRed = frequencyReduction(yGyr, reductionFactor);
        ArrayList<Float> zGyrRed = frequencyReduction(zGyr, reductionFactor);
        ArrayList<Long> timestampRed = frequencyReductionTimestamp(allTimestamp, reductionFactor);

        // Compute gradient of each axis of accelerometer and gyroscope, helpful to detect peaks
        ArrayList<Float> gradXAcc = calculateGradient(xAccRed);
        ArrayList<Float> gradYAcc = calculateGradient(yAccRed);
        ArrayList<Float> gradZAcc = calculateGradient(zAccRed);
        ArrayList<Float> gradXGyr = calculateGradient(xGyrRed);
        ArrayList<Float> gradYGyr = calculateGradient(yGyrRed);
        ArrayList<Float> gradZGyr = calculateGradient(zGyrRed);

        // Compute the norma of gradient along axis
        ArrayList<Float> accNorm = norm(gradXAcc, gradYAcc, gradZAcc);
        ArrayList<Float> gyrNorm = norm(gradXGyr, gradYGyr, gradZGyr);

        // Detection of strokes, returns a list containing the indexes of peaks (express in sample number)
        ArrayList<Integer> strokeDetectedAcc = strokeDetectionAcc(accNorm, userThreshold, userWindowSize, userMinInterval);
        ArrayList<Integer> strokeDetectedGyr = strokeDetectionGyr(strokeDetectedAcc, gyrNorm);

        // Compute strokes' timestamp using the indexes of peaks
        ArrayList<Long> timestampStrokes = takeStrokeTimestamp(timestampRed, strokeDetectedAcc);

        // Classification of strokes of the session
        // 0 --> Forehand
        // 1 --> Backhand
        // 2 --> Smash
        // 3 --> Lob
        int[] totStrokesClassified = new int[4];
        int prediction = 0;
        for(int i=0; i<strokeDetectedAcc.size(); i++){
            // Extract stroke's features
            ArrayList<Float> strokeFeatures = featuresFromPeak(strokeDetectedAcc.get(i), strokeDetectedGyr.get(i), xAccRed, yAccRed, zAccRed, xGyrRed, yGyrRed, zGyrRed);
            // Classify Stroke features
            prediction = classifyStroke(strokeFeatures);

            totStrokesClassified[prediction] += 1;
            strokesPredicted.add(prediction);
        }

        // Extract x and y values from the log
        readLogPositioning(xPositions, yPositions);

        // Attach timestamp for each x and y values from the log
        createTimestampPositions(timestampPositions, timestampStrokes, xPositions.size());

        // Attach strokes to the log position computed by uwb sensor
        computeStrokesCoordinates(timestampStrokes, strokesPredicted, xPositions, yPositions, xStrokes, yStrokes, strokeTimestampPositions, timestampPositions, strokesTypeLog);

        // Get today date
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String todayDate = dtf.format(LocalDateTime.now());

        String dur = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(sessionDuration),
                TimeUnit.MILLISECONDS.toMinutes(sessionDuration) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(sessionDuration)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(sessionDuration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sessionDuration)));

        // Write the current session info to the progress file
        writeToFile("progress.txt", Arrays.toString(totStrokesClassified)+ ";" +
                todayDate + ";"+ dur +";" + xStrokes + ";" + yStrokes + ";" + strokesTypeLog + "\n");

        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, "data_collected.txt");
        readFrom.delete();
    }


    private ArrayList<Long> takeStrokeTimestamp(ArrayList<Long> allTimestamp, ArrayList<Integer> indexStrokes){
        ArrayList<Long> temp = new ArrayList<>();
        for(int i=0; i<indexStrokes.size(); i++){
            temp.add(allTimestamp.get(indexStrokes.get(i)));
        }
        return temp;
    }

    // Creates timestamp for log positions
    private void createTimestampPositions(ArrayList<Long> timestampPositions, ArrayList<Long> timestampStrokes, int lengthPos){
        if(timestampStrokes.size() == 0)
            Log.i(TAG,"There are no strokes detected!!");

        Long initialTimestamp = timestampStrokes.get(0);
        for(int i=0; i<lengthPos; i++) {
            // 100000000L because the positions log are sent every 0.1 seconds
            timestampPositions.add(initialTimestamp + (i * 100000000L));
        }
    }


    private void readLogPositioning(ArrayList<Float> xPositions, ArrayList<Float> yPositions){
        String fileName = "log_position.txt";
        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, fileName);
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(readFrom))) {
            String line;
            while ((line = br.readLine()) != null) {
                count++;
                String[] parts = line.split(",");
                xPositions.add(Float.parseFloat(parts[3]));
                yPositions.add(Float.parseFloat(parts[4]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Associate strokes to positions
    private void computeStrokesCoordinates(ArrayList<Long> timestampStrokes, ArrayList<Integer> strokesType,ArrayList<Float> xPos, ArrayList<Float> yPos,
                                    ArrayList<Float> xStrokes, ArrayList<Float> yStrokes, ArrayList<Long> strokeTimestampPositions,
                                           ArrayList<Long> timestampPositions, ArrayList<Integer> strokesTypeLog){

        Long lastTimestampPositions = timestampPositions.get(timestampPositions.size()-1);
        int indexClosest = 0;

        for(int i=0; i<timestampStrokes.size(); i++){
            if(timestampStrokes.get(i) > lastTimestampPositions){
                Log.d(TAG, "Log positioning shorter than match duration");
                break;
            }
            indexClosest = findNearest(timestampPositions, timestampStrokes.get(i));
            strokeTimestampPositions.add(timestampPositions.get(indexClosest));
            xStrokes.add(xPos.get(indexClosest));
            yStrokes.add(yPos.get(indexClosest));
            strokesTypeLog.add(strokesType.get(i));
        }
    }

    // Return the index of timestamp of positions log closest to the timestamp stroke
    private int findNearest(ArrayList<Long> timestamp, Long timestampReference){
        long distance = Math.abs(timestamp.get(0) - timestampReference);
        int index = 0;
        for(int i = 1; i < timestamp.size(); i++){
            long cdistance = Math.abs(timestamp.get(i) - timestampReference);
            if(cdistance < distance){
                index = i;
                distance = cdistance;
            }
        }
        return index;
    }

    private int classifyStroke(ArrayList<Float> features){
        try {
            TfliteModel model = TfliteModel.newInstance(context);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SIZEOF_FLOAT * features.size());
            byteBuffer.order(ByteOrder.nativeOrder());

            for(int i=0; i<features.size(); i++){
                byteBuffer.putFloat(features.get(i));
            }
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            TfliteModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();
            float maxConf = 0.0f;
            int maxIndex = 0;
            for(int i=0; i<confidence.length; i++){
                if(confidence[i] > maxConf){
                    maxConf = confidence[i];
                    maxIndex = i;
                }
            }
            String[] classes = {"Forehand", "Backhand", "Smash", "Lob"};

            // Releases model resources if no longer used.
            model.close();
            return maxIndex;
        } catch (IOException e) {
            // TODO Handle the exception
        }
        return -1;
    }

    private void writeToFile(String fileName, String content){
        try {
            File f = new File(filePath, fileName);
            FileWriter fw = new FileWriter(f, true);
            fw.append(content);
            fw.close();

            Toast.makeText(context, "Wrote to file " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void readSession(String fileName, ArrayList<Float> xAcc, ArrayList<Float> yAcc,
                             ArrayList<Float> zAcc, ArrayList<Float> xGyr, ArrayList<Float> yGyr, ArrayList<Float> zGyr, ArrayList<Long> timestamp) {
        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, fileName);

        try (BufferedReader br = new BufferedReader(new FileReader(readFrom))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (Float.parseFloat(parts[0]) == 0.0f) {
                    timestamp.add(Long.parseLong(parts[1]));
                    xAcc.add(Float.parseFloat(parts[2]));
                    yAcc.add(Float.parseFloat(parts[3]));
                    zAcc.add(Float.parseFloat(parts[4]));
                } else {
                    xGyr.add(Float.parseFloat(parts[2]));
                    yGyr.add(Float.parseFloat(parts[3]));
                    zGyr.add(Float.parseFloat(parts[4]));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // In order to reduce the sampling rate, we average the data of 3 samples
    private ArrayList<Float> frequencyReduction(ArrayList<Float> data, int reductionFactor){
        ArrayList<Float> dataReducted = new ArrayList<>();
        for(int i = 0; i < data.size(); i = i + reductionFactor){
            Float sum = 0.0f;
            if(i + reductionFactor > data.size()){
                return dataReducted;
            }
            for(int j = 0; j < reductionFactor; j++){
                sum += data.get(i+j);
            }
            dataReducted.add(sum/reductionFactor);

        }
        return dataReducted;
    }

    private ArrayList<Long> frequencyReductionTimestamp(ArrayList<Long> data, int reductionFactor){
        ArrayList<Long> dataReducted = new ArrayList<>();
        for(int i = 0; i < data.size(); i = i + reductionFactor){
            Long sum = 0L;
            if(i + reductionFactor > data.size()){
                return dataReducted;
            }
            for(int j = 0; j < reductionFactor; j++){
                sum += data.get(i+j);
            }
            dataReducted.add(sum/reductionFactor);

        }
        return dataReducted;
    }

    private ArrayList<Float> norm(ArrayList<Float> gradX, ArrayList<Float> gradY, ArrayList<Float> gradZ){
        ArrayList<Float> totGrad = new ArrayList<>();

        for(int i = 0; i < gradX.size(); i++){
            Double num = Math.sqrt(Math.pow(gradX.get(i), 2) + Math.pow(gradY.get(i), 2) + Math.pow(gradZ.get(i), 2));
            Float floatNum = num.floatValue();
            totGrad.add(floatNum);
        }

        return totGrad;
    }

    private ArrayList<Float> calculateGradient(ArrayList<Float> data){
        ArrayList<Float> gradient = new ArrayList<>();

        for(int i = 1; i < data.size(); i++){
            if(i == 1){
                gradient.add((data.get(i) - data.get(i-1)));
            }else{
                gradient.add((data.get(i) - data.get(i-2))/2);
            }
            if(i == data.size() - 1) {
                gradient.add((data.get(i) - data.get(i - 1)));
            }
        }

        return gradient;
    }

    private ArrayList<Float> featuresFromPeak(int accPeak, int gyrPeak, ArrayList<Float> xAcc, ArrayList<Float> yAcc, ArrayList<Float> zAcc, ArrayList<Float> xGyr, ArrayList<Float> yGyr, ArrayList<Float> zGyr){
        ArrayList<Float> features = new ArrayList<>();
        int strokeStartAcc = accPeak - 12;
        int strokeStartGyr = gyrPeak - 12;

        for(int i = 0; i < 25; i++){
            features.add(xAcc.get(i + strokeStartAcc));
            features.add(yAcc.get(i + strokeStartAcc));
            features.add(zAcc.get(i + strokeStartAcc));
            features.add(xGyr.get(i + strokeStartGyr));
            features.add(yGyr.get(i + strokeStartGyr));
            features.add(zGyr.get(i + strokeStartGyr));
        }
        return features;
    }

    private ArrayList<Integer> strokeDetectionGyr(ArrayList<Integer> peaksAcc, ArrayList<Float> gyrNorm){
        ArrayList<Integer> peaksGyr = new ArrayList<>();
        Integer maxIndexGyr = 0;
        Float maxValueGyr = 0.0f;

        for(int i = 0; i < peaksAcc.size(); i++){
            int startSearch = peaksAcc.get(i) - 12;
            for(int j = 0; j < 25; j++){
                if(gyrNorm.get(startSearch + j) > maxValueGyr){
                    maxValueGyr = gyrNorm.get(startSearch + j);
                    maxIndexGyr = startSearch + j;
                }
            }
            peaksGyr.add(maxIndexGyr);
            maxIndexGyr = 0;
            maxValueGyr = 0.0f;
        }
        return peaksGyr;
    }

    private ArrayList<Integer> strokeDetectionAcc(ArrayList<Float> accNorm, int userThreshold, int userWindowSize, int userMinInterval){
        ArrayList<Integer> peakIndexes = new ArrayList<>();


        Float maxValue = 0f;
        int maxIndex = 0;

        for(int i = 0; i < accNorm.size(); i = i + userWindowSize){
            if((i + userWindowSize) > accNorm.size()){
                break;
            }
            for(int j = 0; j < userWindowSize; j++){
                if((accNorm.get(i+j) > maxValue) && (accNorm.get(i+j) > userThreshold)){
                    maxValue = accNorm.get(i+j);
                    maxIndex = i+j;
                }
            }
            if(((maxValue > 0) && (peakIndexes.size() == 0)) || ((maxValue > 0) && (maxIndex - peakIndexes.get(peakIndexes.size() - 1) > userMinInterval))){
                peakIndexes.add(maxIndex);
            }
            maxIndex = 0;
            maxValue = 0f;
        }
        return peakIndexes;
    }


}
