package com.example.mypadel;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mypadel.ml.TfliteModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class StrokeClassification {

    private static final String TAG = "STROKE CLASSIFICATION";
    private static final int reductionFactor = 4;
    private static final int userThreshold = 10;
    private static final int userWindowSize = 30;
    private static final int userMinInterval = 30;
    private int SIZEOF_FLOAT = 4;
    private final Context context;

    public StrokeClassification(){
        context = MainActivity.getContext();
    }

    public void classifySession(){
        String fileName = "data_collected.txt";
        ArrayList<Float> xAcc = new ArrayList<>();
        ArrayList<Float> yAcc = new ArrayList<>();
        ArrayList<Float> zAcc = new ArrayList<>();
        ArrayList<Float> xGyr = new ArrayList<>();
        ArrayList<Float> yGyr = new ArrayList<>();
        ArrayList<Float> zGyr = new ArrayList<>();
        readSession(fileName, xAcc, yAcc, zAcc, xGyr, yGyr, zGyr);
        Log.d(TAG, "Dim tot rows: " + String.valueOf(xAcc.size()));


        ArrayList<Float> xAccRed = frequencyReduction(xAcc, reductionFactor);
        ArrayList<Float> yAccRed = frequencyReduction(yAcc, reductionFactor);
        ArrayList<Float> zAccRed = frequencyReduction(zAcc, reductionFactor);
        ArrayList<Float> xGyrRed = frequencyReduction(xGyr, reductionFactor);
        ArrayList<Float> yGyrRed = frequencyReduction(yGyr, reductionFactor);
        ArrayList<Float> zGyrRed = frequencyReduction(zGyr, reductionFactor);
        Log.d(TAG, "Dim tot reducted row: " + String.valueOf(xAccRed.size()));


        ArrayList<Float> gradXAcc = calculateGradient(xAccRed);
        ArrayList<Float> gradYAcc = calculateGradient(yAccRed);
        ArrayList<Float> gradZAcc = calculateGradient(zAccRed);
        ArrayList<Float> gradXGyr = calculateGradient(xGyrRed);
        ArrayList<Float> gradYGyr = calculateGradient(yGyrRed);
        ArrayList<Float> gradZGyr = calculateGradient(zGyrRed);

        ArrayList<Float> accNorm = norm(gradXAcc, gradYAcc, gradZAcc);
        ArrayList<Float> gyrNorm = norm(gradXGyr, gradYGyr, gradZGyr);

        ArrayList<Integer> strokeDetectedAcc = strokeDetectionAcc(accNorm, userThreshold, userWindowSize, userMinInterval);
        ArrayList<Integer> strokeDetectedGyr = strokeDetectionGyr(strokeDetectedAcc, gyrNorm);

        Log.d(TAG, "Acc peaks: " + strokeDetectedAcc.toString());
        Log.d(TAG, "Gyr peaks: " + strokeDetectedGyr.toString());

        int[] classifiedStrokes = new int[4];
        int prediction = 0;
        for(int i=0; i<strokeDetectedAcc.size(); i++){
            ArrayList<Float> strokeFeatures = featuresFromPeak(strokeDetectedAcc.get(i), strokeDetectedGyr.get(i), xAccRed, yAccRed, zAccRed, xGyrRed, yGyrRed, zGyrRed);
            // classify Stroke fetures
            prediction = classifyStroke(strokeFeatures);
            classifiedStrokes[prediction] += 1;
        }

        Log.d(TAG, "Stroke predicted: " + Arrays.toString(classifiedStrokes));


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

            Log.d(TAG, "Prediction: " + classes[maxIndex] + " --- Confidence: " + maxConf);

            // Releases model resources if no longer used.
            model.close();
            return maxIndex;
        } catch (IOException e) {
            // TODO Handle the exception
        }
        return -1;
    }







    private void readSession(String fileName, ArrayList<Float> xAcc, ArrayList<Float> yAcc, ArrayList<Float> zAcc, ArrayList<Float> xGyr, ArrayList<Float> yGyr, ArrayList<Float> zGyr) {
        File path = context.getExternalFilesDir(null);
        File readFrom = new File(path, fileName);
        //ArrayList<String> accelerometer = new ArrayList<String>();

        try (BufferedReader br = new BufferedReader(new FileReader(readFrom))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (Float.parseFloat(parts[0]) == 0.0f) {
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
