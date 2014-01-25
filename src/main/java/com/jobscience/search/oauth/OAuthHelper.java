package com.jobscience.search.oauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.scribe.builder.ServiceBuilder;
import org.scribe.oauth.OAuthService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.oauth.api.ForceDotComApi;

@Singleton
public class OAuthHelper {
    @Inject
    private ConfigManager configManager;
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private DBSetupManager dbSetupManager;

    private volatile String apiKey = null;
    private volatile String apiSecret = null;
    private volatile String callbackUrl = null;
    
    private Logger log = Logger.getLogger(getClass());

    /**
     * Get the auth service for salesforce,
     * First get the app conifg for current org,if not existed,
     * would use global app config(snow.properties) as default
     * @return
     */
    public OAuthService  getService(){
    	List<Map> list = new ArrayList();
	    if(dbSetupManager.checkSysTables().contains("config")){
	        String sql = "select * from config  where org_id = -1 ";
	        List<Map> configList = daoHelper.executeQuery(daoHelper.openNewSysRunner(), sql);
	        if (configList != null && configList.size() > 0) {
	            list = configList;
	        }
    	}
        list = configManager.checkSaleforceInfo(list);
        if(list != null && list.size() > 0){
            for (Map<String,String> map : list) {
                if ("config_apiKey".equals((String)map.get("name"))) {
                    apiKey = map.get("value");
                } else if ("config_apiSecret".equals((String)map.get("name"))) {
                    apiSecret = map.get("value");
                } else if ("config_callBackUrl".equals((String)map.get("name"))) {
                    callbackUrl = map.get("value");
                }
            }
            ServiceBuilder builder = null;
            OAuthService servcie = null;
            try{
                builder = new ServiceBuilder().provider(ForceDotComApi.class).apiKey(apiKey).apiSecret(apiSecret).callback(callbackUrl);
                servcie = builder.build();
            }catch(Exception e){
                log.error(e.getMessage());
            }
            return servcie;
        }
        return null;
    }

    public String getApiKey() {
        if (apiKey == null) {
            getService();
        }
        return apiKey;
    }

    public String getApiSecret() {
        if (callbackUrl == null) {
            getService();
        }
        return apiSecret;
    }

    public String getCallbackUrl() {
        if (callbackUrl == null) {
            getService();
        }
        return callbackUrl;
    }
}
