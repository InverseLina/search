package com.jobscience.search.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.dao.DatasourceManager;
import com.jobscience.search.perf.AppPerf.Snap;
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
		Map result = new HashMap();
		result.put("appPerf", appPerf);
		
		Comparator<String> c = new Comparator<String>(){
            @Override
            public int compare(String a, String b) {
                if (a == b) {
                    return 0;
                }
                if (a == null) {
                    return -1;
                }
                if (b == null) {
                    return 1;
                }
                
                String aCopy = a.toLowerCase();
                String bCopy = b.toLowerCase();
                
                int length = Math.min(a.length(), b.length());
                for(int i = 0; i < length; i++){
                    char ac = aCopy.charAt(i);
                    char bc = bCopy.charAt(i);
                    if(ac != bc){
                        return ac > bc ? 1 : -1;
                    }
                }
                
                if(a.length() != b.length()){
                    return a.length() > b.length() ? 1 : -1;
                }
                
                return 0;
            }
        };
		
		Map<String, Snap> requestPerfs = appPerf.getRequestsPerf();
		List<String> requestPerfKeys = new ArrayList();
		requestPerfKeys.addAll(requestPerfs.keySet());
		Collections.sort(requestPerfKeys, c);
		result.put("requestPerfSortKeys", requestPerfKeys);
		List<Map> requestPerfValues = new ArrayList();
		for(String key : requestPerfKeys){
		    Map o = new HashMap();
		    o.put("name", key);
		    o.put("value", requestPerfs.get(key));
		    requestPerfValues.add(o);
		}
		result.put("requestPerfSortValues", requestPerfValues);
		
		Map<String, Snap> methodPerfs = appPerf.getMethodsPerf();
        List<String> methodPerfKeys = new ArrayList();
        methodPerfKeys.addAll(methodPerfs.keySet());
        Collections.sort(methodPerfKeys, c);
        result.put("methodPerfSortKeys", methodPerfKeys);
        List<Map> methodPerfValues = new ArrayList();
        for(String key : methodPerfKeys){
            Map o = new HashMap();
            o.put("name", key);
            o.put("value", methodPerfs.get(key));
            methodPerfValues.add(o);
        }
        result.put("methodPerfSortValues", methodPerfValues);
		
		return wrb.success(result);
	}

	@WebPost("/perf-clear")
	public WebResponse clearPerf(){
		perfManager.clear();
		return wrb.success(true);
	}

}
