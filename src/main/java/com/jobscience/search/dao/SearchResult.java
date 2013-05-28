package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

public class SearchResult {
    
    private List<Map> result;
    private long duration;
    private long countDuration;
    private long selectDuration;
    private int count;
    
    public SearchResult(List<Map> result, int count){
        this.result = result;
        this.count = count;
    }    

    public int getCount() {
        return count;
    }
    
    public List<Map> getResult() {
        return result;
    }

    public long getDuration() {
        return duration;
    }
    
    public SearchResult setDuration(long duration){
        this.duration = duration;
        return this;
    }

    public long getCountDuration() {
        return countDuration;
    }
    
    public SearchResult setCountDuration(long countDuration){
        this.countDuration = countDuration;
        return this;
    }

    public long getSelectDuration() {
        return selectDuration;
    }
    
    public SearchResult setSelectDuration(long selectDuration){
        this.selectDuration = selectDuration;
        return this;
    }

    



    

}
