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
    private DBHelper          dbHelper;
    
    @Inject
    private DataSourceManager dsMng;
    @Inject
    private DBSetupManager dbSetupManager;
    public OAuthService  getService(){
    	 List<Map> list = new ArrayList();
	    if(dbSetupManager.checkSysTables()){
	        String sql = "select * from config  order by org_id ";
	        List<Map> configList = dbHelper.executeQuery(dsMng.getSysDataSource(), sql);
	        if (configList != null && configList.size() > 0) {
	            list = configList;
	        }
    	}
        list = configManager.checkSaleforceInfo(list);
        String apiKey = null;
        String apiSecret = null;
        String callbackUrl = null;
        
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
        }
        ServiceBuilder builder = new ServiceBuilder().provider(ForceDotComApi.class).apiKey(apiKey).apiSecret(apiSecret);
        builder.callback(callbackUrl);
        return builder.build();
    }
}
