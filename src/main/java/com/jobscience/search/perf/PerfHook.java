package com.jobscience.search.perf;


import static com.britesnow.snow.web.hook.ReqPhase.START;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.hook.ReqPhase;
import com.britesnow.snow.web.hook.annotation.WebRequestHook;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

/**
 *
 *
 */
@Singleton
public class PerfHook {

    private PerfManager perfManager;
    private Histogram responseDurations ;
    
    @Inject
    public PerfHook(PerfManager perfManager){
        this.perfManager = perfManager;
        responseDurations = perfManager.appMetrics.histogram(MetricRegistry.name(PerfHook.class, "response-duration"));
    }
    
    public Histogram getResponseDurations(){
        return responseDurations;
    }
	@WebRequestHook(phase = START)
	public void startReqPerf(RequestContext rc) {
		RcPerf rcPerf = perfManager.newRcPerf();
		rc.setData(rcPerf);
		rcPerf.startRequest();
	}

	@WebRequestHook(phase = ReqPhase.END)
    public void endReqPerf(RequestContext rc) {
	    RcPerf.RcTimer rcTimer = (RcPerf.RcTimer)(rc.getData(RcPerf.class).getRcPerfInfo().get("req"));
	    responseDurations.update(rcTimer.getDuration());
    }

}
