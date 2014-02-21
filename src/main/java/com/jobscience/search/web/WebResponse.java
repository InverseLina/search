package com.jobscience.search.web;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.jobscience.search.exception.AppException;

import static com.britesnow.snow.util.MapUtil.mapIt;

public class WebResponse {

	private ReqPerf reqPerf = null;

	private Boolean success;
	private Object result;
	private String errorMessage;
	private Throwable t;
	private Map<String, Object> extra;

	protected WebResponse() {
	}


	protected WebResponse(Throwable t) {
		this.t = t;
		this.setSuccess(false);
	}

	WebResponse setReqPerf(ReqPerf reqPerf) {
		this.reqPerf = reqPerf;
		return this;
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


	public WebResponse setT(Throwable t) {
		this.t = t;
		return this;
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

	public Map getPerf(){
		if (reqPerf != null){
			return mapIt("req",reqPerf);
		}else{
			return null;
		}
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

	public WebResponse setErrorMessage(String errorMessage) {
		if (!Strings.isNullOrEmpty(errorMessage)) {
			this.errorMessage = errorMessage;
		} else {
			this.errorMessage = null;
		}
		return this;

	}

	public WebResponse setErrorCode(String errorCode) {
		return this;
	}

	// --------- /Properties --------- //

	// --------- Extra --------- //
	public Map getExtra() {
		return extra;
	}

	public void setExtra(Map extra) {
		this.extra = extra;
	}

	public WebResponse setExtraValue(String name, Object value) {
		if (extra == null) {
			extra = new HashMap<String, Object>();
		}
		extra.put(name, value);
		return this;
	}

	// --------- /Extra --------- //
}
