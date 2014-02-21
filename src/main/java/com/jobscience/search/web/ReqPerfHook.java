package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.hook.On;
import com.britesnow.snow.web.hook.ReqPhase;
import com.britesnow.snow.web.hook.annotation.WebRequestHook;
import com.google.inject.Singleton;

/**
 * Created by jeremychone on 2/21/14.
 */
@Singleton
public class ReqPerfHook {

	static public final String REQ_PERF = "REQ_PERF";

	@WebRequestHook(phase = ReqPhase.START, on = On.BEFORE)
	public void requestStart(RequestContext rc){
		rc.setAttribute(REQ_PERF,new ReqPerf());
	}
}
