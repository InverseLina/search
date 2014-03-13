package com.jobscience.search.perf;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.inject.Singleton;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Inject;

/**
 * <p></p>
 */
@Singleton
public class PerfInterceptor implements MethodInterceptor {

	@Inject
	CurrentRequestContextHolder crch;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {

		RequestContext rc = (crch != null)?crch.getCurrentRequestContext():null;
		RcPerf rcPerf = (rc != null)?rc.getData(RcPerf.class):null;

		String name = methodInvocation.getMethod().getDeclaringClass().getSimpleName() + "." + methodInvocation.getMethod().getName();
		if (rcPerf != null){
			rcPerf.start(name);
		}
		Object r = methodInvocation.proceed();
		if (rcPerf != null){
			rcPerf.end(name);
		}
		return r;
	}
}
