package com.jobscience.search.oauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scribe.builder.ServiceBuilder;
import org.scribe.oauth.OAuthService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;
import com.jobscience.search.oauth.api.ForceDotComApi;

@Singleton
public class OAuthHelper {
    @Inject
    private ConfigManager configManager;
    @Inject
    private DBHelper dbHelper;
    @Inject
    private DataSourceManager dsMng;
    @Inject
    private DBSetupManager dbSetupManager;

    private String apiKey = null;
    private String apiSecret = null;
    private String callbackUrl = null;

    /**
     * Get the auth service for salesforce,
     * First get the app conifg for current org,if not existed,
     * would use global app config(snow.properties) as default
     * @return
     */
    public OAuthService  getService(){
    	List<Map> list = new ArrayList();
	    if(dbSetupManager.checkSysTables()){
	        String sql = "select * from config  where org_id = -1 ";
	        List<Map> configList = dbHelper.executeQuery(dsMng.getSysDataSource(), sql);
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
            ServiceBuilder builder = new ServiceBuilder().provider(ForceDotComApi.class).apiKey(apiKey).apiSecret(apiSecret);
            builder.callback(callbackUrl);
            return builder.build();
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
