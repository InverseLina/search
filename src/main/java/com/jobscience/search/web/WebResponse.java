package com.jobscience.search.web;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.jobscience.search.AppException;

public class WebResponse {

    private Boolean             success;
    private Object              result;
    private String              errorMessage;
    private Throwable           t;
    private Map<String, Object> extra;

    protected WebResponse() {
    }

    protected WebResponse(Throwable t) {
        this.t = t;
        this.setSuccess(false);
    }

    // --------- Static Factories --------- //
    public static WebResponse success() {
        WebResponse wr = new WebResponse();
        wr.setSuccess(true);
        return wr;
    }

    public static WebResponse success(Object result) {
        WebResponse wr = WebResponse.success();
        wr.setResult(result);
        return wr;
    }

    public static WebResponse fail() {
        WebResponse wr = new WebResponse();
        wr.setSuccess(false);
        return wr;
    }

    public static WebResponse fail(String message) {
        WebResponse wr = WebResponse.fail();
        wr.setErrorMessage(message);
        return wr;
    }

    public static WebResponse fail(Throwable t) {
        WebResponse wr = new WebResponse(t);
        return wr;
    }
    // --------- /Static Factories --------- //

    // --------- Properties --------- //
    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorCode() {
        if (t instanceof AppException) {
            return ((AppException) t).getErrorCode();
        } else {
            return null;
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        if (!Strings.isNullOrEmpty(errorMessage)) {
            this.errorMessage = errorMessage;
        } else {
            this.errorMessage = null;
        }

    }
    
    // --------- /Properties --------- //    
    
    // --------- Extra --------- //
    public Map getExtra(){
        return extra;
    }
    
    public void setExtra(Map extra){
        this.extra = extra;
    }
    
    public WebResponse setExtraValue(String name, Object value){
        if (extra == null){
            extra = new HashMap<String,Object>();
        }
        extra.put(name, value);
        return this;
    }
    
    // --------- /Extra --------- //    

}
