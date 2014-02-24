package com.jobscience.search.web;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeremychone on 2/21/14.
 */
public class ReqPerf {

	private Long start = System.currentTimeMillis();

	private Map<String,Long> perfs = new HashMap<String,Long>();

    public Long getTotal(){
		return System.currentTimeMillis() - start;
	}
	
    public void start(String label){
        perfs.put(label, System.currentTimeMillis());
    }
    
    public void stop(String label){
        if(perfs.containsKey(label)){
            perfs.put(label, System.currentTimeMillis()-perfs.get(label));
        }
    }
    public Map getPerfs(){
        return perfs;
    }
}
