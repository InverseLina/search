package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

/**
 * the object of search result
 */
public class SearchResult {
    
    private List<Map> result;
    private long duration;
    private long countDuration;
    private long selectDuration;
    private int count;
    private int pageIdx;
    private int pageSize;
    private boolean exactCount;
    private boolean hasNextPage;
    
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

	public boolean isExactCount() {
		return exactCount;
	}

	public SearchResult setExactCount(boolean exactCount) {
		this.exactCount = exactCount;
		return this;
	}

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public SearchResult setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
        return this;
    }
	
}
