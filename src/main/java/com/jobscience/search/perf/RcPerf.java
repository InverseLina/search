package com.jobscience.search.perf;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * <p>Request Context Performance object. This is created at the beginning of every request and hold the duration for each metrics.</p>
 *
 * <p>So, it keeps a very simplistic name/duration map for the request, and also use the PerformanceManager to track the time accross
 * request (which uses Metrics).</p>
 *
 * <p>Single Thread: This is supposed to be used in conjonction of the RequestContext, and therefore is assumed to be used in
 * a single thread.</p>
 */
public class RcPerf {

	MetricRegistry appMetrics;

	Context startEndContext;

	Map<String,RcTimer> durationByName = new HashMap<String, RcTimer>();

	Long timerOffset = null;

	public RcPerf(MetricRegistry appMetrics) {
		this.appMetrics = appMetrics;
	}

	public void startRequest() {
		start("req");
	}

	public void endRequest(){
		end("req");
	}

	public void start(String name){
		RcTimer rcTimer = durationByName.get(name);
		if (rcTimer != null){
			rcTimer.start();
		}else{
			Timer timer = appMetrics.timer(name);
			Long timerStart;
			if (timerOffset == null){
				timerOffset = System.currentTimeMillis();
				timerStart = timerOffset;
			}else{
				timerStart = System.currentTimeMillis();
			}
			rcTimer = new RcTimer(timer, timerOffset, timerStart);
		}
		durationByName.put(name, rcTimer);
	}

	public void end(String name){
		RcTimer rcTimer = durationByName.get(name);

		if (rcTimer != null){
			rcTimer.end();
		}
	}

	public Map getRcPerfInfo(){
		return durationByName;
	}


	static public class RcTimer{
		private int active = 0;
		private int count = 0;
		private Long requestOffset;

		private Long firstTimerStart;

		private Long timerStart = null;
		private Long timerEnd = null;

		private Long duration = 0L;

		private Timer timer;
		private Context timerContext;


		private RcTimer(Timer timer, Long requestOffset, Long timerStart) {
			this.timer = timer;
			this.requestOffset = requestOffset;
			this.timerStart = timerStart;
			this.firstTimerStart = timerStart;
			start();
		}

		RcTimer start(){
			count++;
			if (active == 0){
				timerStart = System.currentTimeMillis();
				// TODO: probably need to have a FIFO of some sort, to allow multiple start.
				//       right now, the timerContext/metrics logic is a little different than the custom request timer.
				if (timerContext != null){
					timerContext.stop();
				}
				timerContext = timer.time();
			}
			active++;
			return this;
		}

		RcTimer end(){
			active--;
			if (active == 0) {
				timerContext.stop();
				timerEnd = System.currentTimeMillis();
				duration += timerEnd - timerStart;
			}
			return this;
		}


		public Integer getCount(){
			return count;
		}

		/**
		 * Return the relative start from the timerOffset
		 * @return
		 */
		public Long getStart(){
			return firstTimerStart - requestOffset;
		}

		public Long getDuration(){
			return duration;
		}
	}
}
