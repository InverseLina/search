package com.jobscience.search.exception;

import java.sql.SQLException;

public  class JSSSqlException{
	private Integer errorCode;
	private String errorMsg;
	
	public JSSSqlException(Integer errorCode,String errorMsg) {
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}
	
	public JSSSqlException(SQLException e) {
		this.errorCode = e.getErrorCode();
		if(e.getNextException() != null){
		    this.errorMsg = e.getNextException().getMessage();
		}else{
		    this.errorMsg = e.getMessage();
		}
	}
	
	public Integer getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}
	
	public String getErrorMsg() {
		return errorMsg;
	}
	
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
}
