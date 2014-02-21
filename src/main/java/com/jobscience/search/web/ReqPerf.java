package com.jobscience.search.web;

/**
 * Created by jeremychone on 2/21/14.
 */
public class ReqPerf {

	private Long start = System.currentTimeMillis();


	public Long getTotal(){
		return System.currentTimeMillis() - start;
	}
}
