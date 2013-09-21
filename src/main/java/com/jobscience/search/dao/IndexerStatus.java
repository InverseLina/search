package com.jobscience.search.dao;
public class IndexerStatus{
	private int remaining;
	private int perform;
	public IndexerStatus(int remaining,int perform){
		this.remaining = remaining;
		this.perform = perform;
	}
	public int getRemaining() {
		return remaining;
	}
	public void setRemaining(int remaining) {
		this.remaining = remaining;
	}
	public int getPerform() {
		return perform;
	}
	public void setPerform(int perform) {
		this.perform = perform;
	}
	
}