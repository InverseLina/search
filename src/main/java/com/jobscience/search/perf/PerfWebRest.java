package com.jobscience.search.perf;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.dao.DatasourceManager;
import com.jobscience.search.web.WebResponse;
import com.jobscience.search.web.WebResponseBuilder;

/**
 * <p>WebRest methods to get and refresh the Perf info</p>
 */
@Singleton
public class PerfWebRest {

	@Inject
	private WebResponseBuilder wrb;

	@Inject
	private PerfManager perfManager;

	@Inject
	DatasourceManager datasourceManager;

	@WebGet("/perf-get-all")
	public WebResponse getAllPerf(){

		AppPerf appPerf = perfManager.getAppPerf(datasourceManager.getPoolInfo());

		return wrb.success(appPerf);
	}

	@WebPost("/perf-clear")
	public WebResponse clearPerf(){
		perfManager.clear();
		return wrb.success(true);
	}

}
