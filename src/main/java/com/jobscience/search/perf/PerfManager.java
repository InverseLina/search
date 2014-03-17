package com.jobscience.search.perf;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * <p>Preformance Manager </p>
 */
public class PerfManager {

	final MetricRegistry appMetrics = new MetricRegistry();

	RcPerf newRcPerf(){
		return new RcPerf();
	}

	public PerfContext start(String name){
		Timer timer = appMetrics.timer(name);
		final Timer.Context ctx = timer.time();

		return new PerfContext(){
			@Override
			public void end() {
				ctx.stop();
			}
		};
	}

	public static interface PerfContext{
		public void end();
	}

}
