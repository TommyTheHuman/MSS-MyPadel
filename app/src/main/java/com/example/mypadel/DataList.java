package com.example.mypadel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataList implements Serializable {
    private List<SensedData> list;

    DataList(){
        list = new ArrayList<>();
    }

    public void addElement(SensedData sd){
        list.add(sd);
    }
    public List<SensedData> getList(){
        return list;
    }

}
