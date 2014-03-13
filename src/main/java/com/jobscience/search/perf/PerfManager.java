package com.jobscience.search.perf;

import com.codahale.metrics.MetricRegistry;

/**
 * <p>Preformance Manager </p>
 */
public class PerfManager {

	final MetricRegistry appMetrics = new MetricRegistry();

	RcPerf newRcPerf(){
		return new RcPerf(appMetrics);
	}
}
