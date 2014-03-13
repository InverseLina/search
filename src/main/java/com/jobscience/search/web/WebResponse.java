package com.jobscience.search.web;

import java.util.Map;

import com.jobscience.search.exception.AppException;

public class WebResponse {
    
    private Boolean success;
    private Object result;
    private Throwable throwable;
    private Map perf;
    
    
    WebResponse(){
    }

    public Boolean getSuccess() {
        return success;
    }
    public WebResponse setSuccess(Boolean success) {
        this.success = success;
        return this;
    }


    public Object getResult() {
        return result;
    }
    public WebResponse setResult(Object result) {
        this.result = result;
        return this;
    }

    public Throwable getThrowable() {
        return throwable;
    }
    public WebResponse setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    public Map getPerf() {
        return perf;
    }
    void setPerf(Map perf) {
        this.perf = perf;
    }

    public String getErrorCode(){
        if (throwable instanceof AppException){
            return ((AppException) throwable).getErrorCode();
        }else{
            return null;
        }
    }
    
    public String getErrorMessage() {
        if(throwable != null){
            return throwable.getMessage();
        }
        return null;
    }

}
