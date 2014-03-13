package com.jobscience.search.perf;


import static com.britesnow.snow.web.hook.ReqPhase.START;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.hook.annotation.WebRequestHook;

/**
 *
 *
 */
@Singleton
public class PerfHook {



	@Inject
	private PerfManager perfManager;

	@WebRequestHook(phase = START)
	public void startReqPerf(RequestContext rc) {
		RcPerf rcPerf = perfManager.newRcPerf();
		rc.setData(rcPerf);
		rcPerf.startRequest();
	}



}
