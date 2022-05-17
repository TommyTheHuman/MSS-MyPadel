package com.example.mypadel;

import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.*;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;



public class ResultFragment extends Fragment {


    private TextView tvData;
    private TextView tvDuration;
    private String data;
    private String duration;
    private String[] strokes;
    private ArrayList<String> xPos;
    private ArrayList<String> yPos;
    private ArrayList<String> strokeTypes;


    public PieChart pie;

    private Segment s1;
    private Segment s2;
    private Segment s3;
    private Segment s4;

    private XYPlot plot;

    private ArrayList<Float> xFore;
    private ArrayList<Float> yFore;
    private ArrayList<Float> xBack;
    private ArrayList<Float> yBack;
    private ArrayList<Float> xSmas;
    private ArrayList<Float> ySmas;
    private ArrayList<Float> xLob;
    private ArrayList<Float> yLob;

    public ResultFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        strokes = new String[4];
        xPos = new ArrayList<>();
        yPos = new ArrayList<>();
        strokeTypes = new ArrayList<>();
        xFore = new ArrayList<>();
        yFore = new ArrayList<>();
        xBack = new ArrayList<>();
        yBack = new ArrayList<>();
        xSmas = new ArrayList<>();
        ySmas = new ArrayList<>();
        xLob = new ArrayList<>();
        yLob = new ArrayList<>();

        return inflater.inflate(R.layout.result_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        String info = getArguments().getString("info");
        extractInfo(info);

        createPieChart(view);
        createXYPlot(view);

    }

    private void createXYPlot(View view){
        plot = (XYPlot) getView().findViewById(R.id.plot);

        XYSeries series1 = generateScatter("Forehand", xFore, yFore);
        XYSeries series2 = generateScatter("Backhand", xBack, yBack);
        XYSeries series3 = generateScatter("Smash", xSmas, ySmas);
        XYSeries series4 = generateScatter("Lob", xLob, yLob);


        plot.setDomainBoundaries(0, 5, BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, 10, BoundaryMode.FIXED);


        // add each series to the xyplot:
        plot.addSeries(series1, new LineAndPointFormatter(null,Color.GREEN,Color.GREEN,null));
        plot.addSeries(series2, new LineAndPointFormatter(null,Color.BLUE,Color.BLUE,null));
        plot.addSeries(series3, new LineAndPointFormatter(null,Color.BLACK,Color.BLACK,null));
        plot.addSeries(series4, new LineAndPointFormatter(null,Color.RED,Color.RED,null));
    }

    private XYSeries generateScatter(String title, ArrayList<Float> x, ArrayList<Float> y) {
        SimpleXYSeries series = new SimpleXYSeries(title);
        for(int i = 0; i < x.size(); i++) {
            series.addLast(
                    x.get(i),
                    y.get(i)
            );
        }
        return series;
    }

    private void createPieChart(View view){

        tvData = (TextView) getView().findViewById(R.id.textDate);
        tvDuration = (TextView) getView().findViewById(R.id.textDuration);
        tvData.setText(data);
        tvDuration.setText(duration);

        pie = (PieChart) getView().findViewById(R.id.pieChart);

        s1 = new Segment("Forehand", Integer.valueOf(strokes[0]));
        s2 = new Segment("Backhand", Integer.valueOf(strokes[1]));
        s3 = new Segment("Smash", Integer.valueOf(strokes[2]));
        s4 = new Segment("Lob", Integer.valueOf(strokes[3]));

        EmbossMaskFilter emf = new EmbossMaskFilter(
                new float[]{1, 1, 1}, 0.4f, 10, 8.2f);

        SegmentFormatter sf1 = new SegmentFormatter(Color.YELLOW, Color.YELLOW);
        sf1.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sf1.getFillPaint().setMaskFilter(emf);
        sf1.getLabelPaint().setTextSize(35f);
        sf1.getLabelPaint().setColor(Color.BLACK);

        SegmentFormatter sf2 = new SegmentFormatter(Color.BLUE, Color.BLUE);
        sf2.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sf2.getFillPaint().setMaskFilter(emf);
        sf2.getLabelPaint().setTextSize(35f);
        sf2.getLabelPaint().setColor(Color.BLACK);


        SegmentFormatter sf3 = new SegmentFormatter(Color.GREEN, Color.GREEN);
        sf3.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sf3.getLabelPaint().setTextSize(35f);
        sf3.getLabelPaint().setColor(Color.BLACK);

        SegmentFormatter sf4 = new SegmentFormatter(Color.RED, Color.RED);
        sf4.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sf4.getLabelPaint().setTextSize(35f);
        sf4.getLabelPaint().setColor(Color.BLACK);

        pie.addSegment(s1, sf1);
        pie.addSegment(s2, sf2);
        pie.addSegment(s3, sf3);
        pie.addSegment(s4, sf4);

        pie.getBorderPaint().setColor(Color.TRANSPARENT);
        pie.getBackgroundPaint().setColor(Color.TRANSPARENT);
    }



    private void extractInfo(String info){
        String[] aux = info.split(";");
        data = aux[1];
        duration = aux[2];
        String auxStrokes = aux[0];
        auxStrokes = auxStrokes.substring(1, auxStrokes.length()-1);
        strokes = auxStrokes.split(",");

        // delete first whitespace before array numbers
        for(int i=1; i<4; i++){
            strokes[i] = strokes[i].substring(1);
        }

        String[] auxXPos = aux[3].substring(1, aux[3].length()-1).split(",");
        String[] auxYPos = aux[4].substring(1, aux[4].length()-1).split(",");
        String[] auxTypeStroke = aux[5].substring(1, aux[5].length()-1).split(",");


        // delete first whitespace before array numbers
        for(int i=1; i<auxYPos.length; i++){
            auxXPos[i] = auxXPos[i].substring(1);
            auxYPos[i] = auxYPos[i].substring(1);
            auxTypeStroke[i] = auxTypeStroke[i].substring(1);
        }


        xPos.addAll(Arrays.asList(auxXPos));
        yPos.addAll(Arrays.asList(auxYPos));
        strokeTypes.addAll(Arrays.asList(auxTypeStroke));

        for(int i=0; i<xPos.size(); i++){
            if(strokeTypes.get(i).equals("0")){
                xFore.add(Float.valueOf(xPos.get(i)));
                yFore.add(Float.valueOf(yPos.get(i)));
            }
            if(strokeTypes.get(i).equals("1")){
                xBack.add(Float.valueOf(xPos.get(i)));
                yBack.add(Float.valueOf(yPos.get(i)));
            }
            if(strokeTypes.get(i).equals("2")){
                xSmas.add(Float.valueOf(xPos.get(i)));
                ySmas.add(Float.valueOf(yPos.get(i)));
            }
            if(strokeTypes.get(i).equals("3")){
                xLob.add(Float.valueOf(xPos.get(i)));
                yLob.add(Float.valueOf(yPos.get(i)));
            }
        }
    }


}