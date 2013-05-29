package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

public class SearchResult {
    
    private List<Map> result;
    private long duration;
    private long countDuration;
    private long selectDuration;
    private int count;
    private int pageIdx;
    private int pageSize;
    
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

    public int getPageIdx() {
        return pageIdx;
    }

    public void setPageIdx(int pageIdx) {
        this.pageIdx = pageIdx;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
