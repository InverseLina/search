package com.jobscience.search.web;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 *
 * <p>This is the WebResponse builder that should be used now. It get the ReqPerf from the current RequestContext.
 * </p>
 *
 * Created by jeremychone on 2/21/14.
 */
@Singleton
public class WebResponseBuilder {

	@Inject
	CurrentRequestContextHolder crch;

	// --------- WebResponse Factories --------- //
	public WebResponse success() {
		return newSuccessWebResponse();
	}

	public WebResponse success(Object result) {
		return newSuccessWebResponse().setResult(result);
	}

	public WebResponse fail() {
		return newFailWebResponse();
	}

	public WebResponse fail(String message) {
		return newFailWebResponse().setErrorMessage(message);
	}

	public WebResponse fail(Throwable t) {
		return newFailWebResponse().setT(t);
	}
	// --------- /WebResponse Factories --------- //

	private WebResponse newSuccessWebResponse(){
		WebResponse wr = new WebResponse();
		wr.setSuccess(true);
		return wr.setReqPerf(crch.getCurrentRequestContext().getAttributeAs(ReqPerfHook.REQ_PERF,ReqPerf.class));
	}

	private WebResponse newFailWebResponse(){
		WebResponse wr = new WebResponse();
		wr.setSuccess(false);
		return wr.setReqPerf(crch.getCurrentRequestContext().getAttributeAs(ReqPerfHook.REQ_PERF,ReqPerf.class));
	}
}
