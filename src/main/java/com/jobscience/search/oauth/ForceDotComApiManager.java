package com.jobscience.search.oauth;

import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.oauth.api.ForceDotComApi;


@Singleton
public class ForceDotComApiManager {

    private ConcurrentHashMap<Integer,ForceDotComApi> apiCache = new ConcurrentHashMap();
    
    @Inject
    private ConfigManager configManager;
    
    public ForceDotComApi getForceDotComApi(Integer orgId){
        ForceDotComApi api = apiCache.get(orgId);
        if(api != null){
            return api;
        }
        
        String force_login_url = configManager.getConfig("force_login_url", orgId);
        api = new ForceDotComApi(force_login_url);
        apiCache.put(orgId, api);
        return api;
    }
    
    public void clearForceDotComApi(Integer orgId){
        apiCache.remove(orgId);
    }
}
