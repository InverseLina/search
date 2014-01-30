package com.jobscience.search.oauth;

import java.util.List;
import java.util.Map;
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
        
        List<Map> list = configManager.getConfig("force_login_url", orgId);
        if(list.size() > 0){
            String force_login_url = null;
            for(Map config : list){
                String name = (String) config.get("name");
                if(name != null && name.equals("force_login_url")){
                    force_login_url = (String) config.get("value");
                    break;
                }
            }
            api = new ForceDotComApi(force_login_url);
            apiCache.put(orgId, api);
            return api;
        }
        return null;
    }
    
    public void clearForceDotComApi(Integer orgId){
        apiCache.remove(orgId);
    }
}
