package com.jobscience.search.perf;

import javax.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.inject.Singleton;
import com.jobscience.search.perf.PerfManager.PerfContext;

/**
 * <p></p>
 */
@Singleton
public class PerfInterceptor implements MethodInterceptor {

	@Inject
	PerfManager perfManager;

	@Inject
	CurrentRequestContextHolder crch;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Object result;

		String name = methodInvocation.getMethod().getDeclaringClass().getSimpleName() + "." + methodInvocation.getMethod().getName();

		RequestContext rc = (crch != null)?crch.getCurrentRequestContext():null;
		RcPerf rcPerf = (rc != null)?rc.getData(RcPerf.class):null;

		PerfContext requestPrefContext = (rcPerf != null)?rcPerf.start(name):null;
		PerfContext appPerfContext = (perfManager != null)?perfManager.startMethod(name):null;
		try{
			result = methodInvocation.proceed();
		}finally{
			if (appPerfContext != null) appPerfContext.end();
			if (requestPrefContext != null) requestPrefContext.end();
		}
		return result;


	}
}
