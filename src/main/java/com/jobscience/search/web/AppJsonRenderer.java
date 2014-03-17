package com.jobscience.search.web;

import java.io.Writer;

import javax.inject.Inject;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.renderer.JsonLibJsonRenderer;
import com.britesnow.snow.web.renderer.JsonRenderer;
import com.jobscience.search.perf.RcPerf;

/**
 * <p></p>
 */
public class AppJsonRenderer implements JsonRenderer{

	@Inject
	private CurrentRequestContextHolder crch;

	@Inject
	private JsonLibJsonRenderer jsonLibRenderer;

	@Override
	public void render(Object data, Writer out) {
		RequestContext rc = crch.getCurrentRequestContext();

		if (rc != null && data instanceof WebResponse) {
			WebResponse webResponse = (WebResponse) data;
			RcPerf rcPerf = rc.getData(RcPerf.class);
			rcPerf.endRequest();
			webResponse.setPerf(rcPerf.getRcPerfInfo());
		}

		jsonLibRenderer.render(data,out);
	}
}
